#!/usr/bin/env python3
"""Offline tamper regression fixtures for the build attestation verifier."""
from __future__ import annotations

import hashlib
import json
import pathlib
import subprocess
import sys
import tempfile
import zipfile

ROOT = pathlib.Path(__file__).resolve().parents[1]
VERSION = next(
    line.partition("=")[2] for line in (ROOT / "gradle.properties").read_text().splitlines()
    if line.startswith("mod_version=")
).strip()
HEAD = subprocess.check_output(["git", "rev-parse", "HEAD"], text=True, cwd=ROOT).strip()
VERIFY = ROOT / "scripts" / "verify_artifact_provenance.py"


def verify(jar: pathlib.Path, receipt: pathlib.Path, accepted: bool) -> None:
    result = subprocess.run([sys.executable, str(VERIFY), str(jar), str(receipt)],
                            cwd=ROOT, text=True, capture_output=True, timeout=30)
    if (result.returncode == 0) != accepted:
        raise AssertionError(
            f"provenance expected accepted={accepted}, returncode={result.returncode}: "
            f"{result.stdout}\n{result.stderr}"
        )


def main() -> None:
    with tempfile.TemporaryDirectory(prefix="canvas-provenance-") as temp:
        d = pathlib.Path(temp)
        jar = d / "canvas.jar"
        with zipfile.ZipFile(jar, "w") as archive:
            archive.writestr("fabric.mod.json", json.dumps({
                "schemaVersion": 1, "id": "canvas", "version": VERSION,
                "depends": {"minecraft": "~26.2"},
            }))
        baseline = {
            "schema": 1, "commit": HEAD, "minecraft": "26.2",
            "version": VERSION,
            "runtime_sha256": hashlib.sha256(jar.read_bytes()).hexdigest(),
            "runtime_bytes": jar.stat().st_size,
        }
        receipt = d / "receipt.json"
        receipt.write_text(json.dumps(baseline))
        verify(jar, receipt, True)

        def reject(changed: dict) -> None:
            receipt.write_text(json.dumps(changed))
            verify(jar, receipt, False)

        reject({**baseline, "commit": "f" * 40})
        reject({**baseline, "runtime_sha256": "0" * 64})
        reject({**baseline, "runtime_bytes": baseline["runtime_bytes"] + 1})
        reject({**baseline, "version": "0.0.0-substituted"})
        reject({**baseline, "minecraft": "1.21.1"})
        reject({k: v for k, v in baseline.items() if k != "commit"})
        receipt.write_text(json.dumps(baseline))
        jar.write_bytes(jar.read_bytes() + b"changed")
        verify(jar, receipt, False)
        print("build attestation tamper fixtures: PASS (7 cases rejected)")


if __name__ == "__main__":
    main()
