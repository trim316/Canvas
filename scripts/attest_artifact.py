#!/usr/bin/env python3
"""Record a content-bound build attestation on the runner that built the tested JAR."""
from __future__ import annotations

import hashlib
import json
import pathlib
import subprocess
import sys
import zipfile

if len(sys.argv) != 3:
    raise SystemExit("usage: attest_artifact.py <runtime-jar> <receipt-json>")
jar = pathlib.Path(sys.argv[1])
out = pathlib.Path(sys.argv[2])
props = dict(
    line.split("=", 1) for line in pathlib.Path("gradle.properties").read_text(
        encoding="utf-8"
    ).splitlines() if "=" in line and not line.lstrip().startswith("#")
)
commit = subprocess.check_output(["git", "rev-parse", "HEAD"], text=True).strip()
with zipfile.ZipFile(jar) as archive:
    meta = json.loads(archive.read("fabric.mod.json"))
if meta.get("id") != "canvas" or meta.get("version") != props["mod_version"]:
    raise SystemExit("built runtime Fabric identity differs from checked-in version")
if meta.get("depends", {}).get("minecraft") != "~26.2":
    raise SystemExit("built runtime Minecraft constraint differs from 26.2")
receipt = {
    "schema": 1,
    "commit": commit,
    "minecraft": "26.2",
    "version": props["mod_version"],
    "runtime_sha256": hashlib.sha256(jar.read_bytes()).hexdigest(),
    "runtime_bytes": jar.stat().st_size,
}
out.parent.mkdir(parents=True, exist_ok=True)
out.write_text(json.dumps(receipt, indent=2, sort_keys=True) + "\n", encoding="utf-8")
print("runtime build attestation: PASS", commit, receipt["runtime_sha256"])
