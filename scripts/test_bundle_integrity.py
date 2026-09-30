#!/usr/bin/env python3
"""Black-box adversarial regression tests for the Canvas distributable ZIP.

Uses synthetic Fabric JARs and never accesses a player's Minecraft installation.
"""
from __future__ import annotations

import hashlib
import io
import json
import pathlib
import subprocess
import sys
import tempfile
import zipfile

ROOT = pathlib.Path(__file__).resolve().parents[1]
PACK = ROOT / "scripts" / "package_candidate.py"
VERIFY = ROOT / "scripts" / "verify_bundle.py"


def fixture_jar(path: pathlib.Path, mod_id: str) -> None:
    with zipfile.ZipFile(path, "w") as z:
        z.writestr("fabric.mod.json", json.dumps({
            "schemaVersion": 1, "id": mod_id, "version": "1.0",
            "name": mod_id, "environment": "*",
        }))


def check(zip_path: pathlib.Path, should_pass: bool) -> None:
    result = subprocess.run(
        [sys.executable, str(VERIFY), str(zip_path)],
        cwd=ROOT, text=True, stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT, timeout=60,
    )
    if (result.returncode == 0) != should_pass:
        raise AssertionError(
            f"verify_bundle expected pass={should_pass} got exit "
            f"{result.returncode}: {result.stdout}"
        )


def altered(source: pathlib.Path, target: pathlib.Path, changes: dict[str, bytes],
            extras: dict[str, bytes] | None = None) -> None:
    with zipfile.ZipFile(source) as original:
        members = {name: original.read(name) for name in original.namelist()}
    members.update(changes)
    members.update(extras or {})
    with zipfile.ZipFile(target, "w", compression=zipfile.ZIP_DEFLATED) as z:
        for name, payload in members.items():
            z.writestr(name, payload)


def main() -> None:
    with tempfile.TemporaryDirectory(prefix="canvas-integrity-") as temp:
        d = pathlib.Path(temp)
        candidate = d / "candidate"
        companions = d / "companions"
        candidate.mkdir()
        companions.mkdir()
        fixture_jar(candidate / "canvas-fixture.jar", "canvas")
        fixture_jar(companions / "ambient-fixture.jar", "ambient_fixture")
        fixture_jar(companions / "audio-fixture.jar", "audio_fixture")
        (companions / "companion-admission.json").write_text(json.dumps({
            "schema": 1,
            "admitted_roots": ["ambient-fixture", "audio-fixture"],
            "quarantined_roots": [],
            "active_entries": [
                {"slug": "ambient-fixture", "filename": "ambient-fixture.jar"},
                {"slug": "audio-fixture", "filename": "audio-fixture.jar"},
            ]
        }), encoding="utf-8")
        good = d / "good.zip"
        created = subprocess.run(
            [sys.executable, str(PACK), str(candidate), str(companions), str(good)],
            cwd=ROOT, text=True, stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT, timeout=60,
        )
        if created.returncode:
            raise AssertionError(created.stdout)
        check(good, True)

        with zipfile.ZipFile(good) as z:
            names = z.namelist()
            manifest = json.loads(z.read("INSTALL-MANIFEST.json"))
            canvas_entry = next(x for x in manifest["mods"] if x["mod_id"] == "canvas")
            canvas_path = "mods/" + canvas_entry["filename"]
            payload = z.read(canvas_path)
            original_checksums = z.read("SHA256SUMS.txt")
        assertions = []

        tampered = d / "tampered-payload.zip"
        altered(good, tampered, {canvas_path: payload + b"unsanctioned"})
        check(tampered, False)
        assertions.append("payload hash alteration rejected")

        erased = d / "unlisted-mod.zip"
        extras = {"mods/unlisted-fixture.jar": (companions / "audio-fixture.jar").read_bytes()}
        altered(good, erased, {}, extras)
        check(erased, False)
        assertions.append("unlisted mod rejected")

        forged = d / "forged-manifest.zip"
        forged_manifest = json.loads(json.dumps(manifest))
        canvas = next(x for x in forged_manifest["mods"] if x["mod_id"] == "canvas")
        canvas["sha256"] = "0" * 64
        altered(good, forged, {
            "INSTALL-MANIFEST.json": json.dumps(forged_manifest).encode("utf-8")
        })
        check(forged, False)
        assertions.append("forged manifest rejected")

        missing = d / "missing-checksum.zip"
        altered(good, missing, {"SHA256SUMS.txt": b""})
        check(missing, False)
        assertions.append("missing checksum coverage rejected")

        wrongid = d / "forged-id.zip"
        wrong_manifest = json.loads(json.dumps(manifest))
        next(x for x in wrong_manifest["mods"] if x["mod_id"] == "canvas")["mod_id"] = "counterfeit"
        altered(good, wrongid, {
            "INSTALL-MANIFEST.json": json.dumps(wrong_manifest).encode("utf-8")
        })
        check(wrongid, False)
        assertions.append("counterfeit Fabric ID rejected")

        duplicate = d / "duplicate-path.zip"
        with zipfile.ZipFile(good) as inp, zipfile.ZipFile(duplicate, "w") as out:
            for item in inp.infolist():
                out.writestr(item.filename, inp.read(item))
            out.writestr(canvas_path, payload)
        check(duplicate, False)
        assertions.append("duplicate archive path rejected")

        print("Bundle adversarial integrity: PASS")
        for detail in assertions:
            print(" - " + detail)


if __name__ == "__main__":
    main()
