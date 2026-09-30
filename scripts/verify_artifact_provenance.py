#!/usr/bin/env python3
"""Fail closed when a packaged JAR does not match the actual tested build receipt."""
from __future__ import annotations

import hashlib
import json
import pathlib
import re
import subprocess
import sys
import zipfile

if len(sys.argv) != 3:
    raise SystemExit("usage: verify_artifact_provenance.py <downloaded-runtime-jar> <attestation-json>")
jar = pathlib.Path(sys.argv[1])
receipt_path = pathlib.Path(sys.argv[2])
receipt = json.loads(receipt_path.read_text(encoding="utf-8"))
expected = {"schema", "commit", "minecraft", "version", "runtime_sha256", "runtime_bytes"}
if set(receipt) != expected or receipt["schema"] != 1:
    raise SystemExit("invalid or incomplete build attestation")
commit = subprocess.check_output(["git", "rev-parse", "HEAD"], text=True).strip()
if receipt["commit"] != commit or not re.fullmatch("[0-9a-f]{40}", str(receipt["commit"])):
    raise SystemExit("artifact originated from a different source commit")
properties = dict(
    line.split("=", 1) for line in pathlib.Path("gradle.properties").read_text(
        encoding="utf-8"
    ).splitlines() if "=" in line and not line.lstrip().startswith("#")
)
if receipt["minecraft"] != "26.2" or receipt["version"] != properties.get("mod_version"):
    raise SystemExit("artifact provenance version or Minecraft target drift")
if not jar.is_file() or receipt["runtime_bytes"] != jar.stat().st_size:
    raise SystemExit("runtime artifact size differs from tested build")
if receipt["runtime_sha256"] != hashlib.sha256(jar.read_bytes()).hexdigest():
    raise SystemExit("runtime artifact digest differs from tested build")
with zipfile.ZipFile(jar) as archive:
    meta = json.loads(archive.read("fabric.mod.json"))
if meta.get("id") != "canvas" or meta.get("version") != receipt["version"]:
    raise SystemExit("embedded Fabric runtime identity drift")
print("artifact build provenance: PASS", commit, receipt["runtime_sha256"])
