#!/usr/bin/env python3
"""Exercise the packaged Canvas Windows install/update/restore flow in a throwaway profile."""
from __future__ import annotations

import hashlib
import json
import os
import pathlib
import shutil
import subprocess
import sys
import tempfile
import zipfile

ROOT = pathlib.Path(__file__).resolve().parents[1]


def jar(path: pathlib.Path, mod_id: str, version: str) -> None:
    with zipfile.ZipFile(path, "w") as z:
        z.writestr("fabric.mod.json", json.dumps({
            "schemaVersion": 1, "id": mod_id, "version": version,
            "name": mod_id, "environment": "*"
        }))


def invoke(cmd: pathlib.Path, appdata: pathlib.Path, expected_success: bool) -> str:
    env = os.environ.copy()
    env["APPDATA"] = str(appdata)
    completed = subprocess.run(
        ["cmd", "/d", "/c", str(cmd)], cwd=cmd.parent, env=env,
        stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True,
        timeout=120,
    )
    if (completed.returncode == 0) != expected_success:
        raise AssertionError(
            f"{cmd.name} expected_success={expected_success} "
            f"returncode={completed.returncode}\n{completed.stdout}"
        )
    return completed.stdout


def main() -> None:
    if sys.platform != "win32":
        raise SystemExit("Windows-only installer harness must run on windows-2022")
    with tempfile.TemporaryDirectory(prefix="canvas-windows-install-") as tmp:
        root = pathlib.Path(tmp)
        appdata = root / "AppData"
        mods = appdata / "ModrinthApp" / "profiles" / "Fabulously Optimized" / "mods"
        mods.mkdir(parents=True)
        old = mods / "old-canvas.jar"
        jar(old, "canvas", "0.1.0-old")
        old_digest = hashlib.sha256(old.read_bytes()).hexdigest()

        candidate = root / "candidate"
        companions = root / "companions"
        candidate.mkdir()
        companions.mkdir()
        jar(candidate / "canvas-new.jar", "cozycanvas", "0.2.1-rc.1")
        jar(companions / "ambient.jar", "ambient_ci_fixture", "1.0")
        jar(companions / "acoustic.jar", "acoustic_ci_fixture", "1.0")
        report = {
            "schema": 1, "admitted_roots": ["fixture-ambient", "fixture-acoustic"],
            "quarantined_roots": [],
            "active_entries": [
                {"slug": "fixture-ambient", "filename": "ambient.jar"},
                {"slug": "fixture-acoustic", "filename": "acoustic.jar"}
            ],
        }
        (companions / "companion-admission.json").write_text(json.dumps(report), encoding="utf-8")

        bundle = root / "dist" / "Canvas-fixture.zip"
        build = subprocess.run(
            [sys.executable, str(ROOT / "scripts" / "package_candidate.py"),
             str(candidate), str(companions), str(bundle)],
            cwd=ROOT, capture_output=True, text=True, timeout=120,
        )
        if build.returncode:
            raise AssertionError(build.stdout + "\n" + build.stderr)

        stage = bundle.parent / "candidate-stage"
        # A modified input JAR must fail before any old installed JAR moves.
        incoming = stage / "mods" / "canvas-new.jar"
        baseline = incoming.read_bytes()
        incoming.write_bytes(b"tampered payload")
        invoke(stage / "INSTALL-CANVAS.cmd", appdata, expected_success=False)
        if hashlib.sha256(old.read_bytes()).hexdigest() != old_digest:
            raise AssertionError("Hash rejection modified the existing profile")
        incoming.write_bytes(baseline)

        invoke(stage / "INSTALL-CANVAS.cmd", appdata, expected_success=True)
        if old.exists() or not (mods / "canvas-new.jar").exists():
            raise AssertionError("Update failed to replace the old Canvas JAR")
        backup_root = mods / ".canvas-backup"
        backups = [p for p in backup_root.iterdir() if p.is_dir()]
        if len(backups) != 1 or not (backups[0] / "old-canvas.jar").exists():
            raise AssertionError("Update did not preserve the replaced Canvas JAR")

        invoke(stage / "RESTORE-CANVAS.cmd", appdata, expected_success=True)
        if not old.exists() or (mods / "canvas-new.jar").exists():
            raise AssertionError("Restore failed to reinstate the original Canvas JAR")
        if hashlib.sha256(old.read_bytes()).hexdigest() != old_digest:
            raise AssertionError("Restore did not preserve original JAR bytes")
        invoke(stage / "RESTORE-CANVAS.cmd", appdata, expected_success=False)
        print("Windows installer/rollback: PASS (tamper, update, backup, restore, double restore)")


if __name__ == "__main__":
    main()
