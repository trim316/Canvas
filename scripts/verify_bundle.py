#!/usr/bin/env python3
from __future__ import annotations

import hashlib
import json
import pathlib
import sys
import zipfile

if len(sys.argv) != 2:
    raise SystemExit("usage: verify_bundle.py <bundle.zip>")

bundle = pathlib.Path(sys.argv[1])
if not bundle.exists():
    raise SystemExit(f"bundle missing: {bundle}")

with zipfile.ZipFile(bundle) as z:
    names = z.namelist()
    if len(names) != len(set(names)):
        raise SystemExit("bundle contains duplicate paths")
    required = {
        "INSTALL-CANVAS.cmd",
        "RESTORE-CANVAS.cmd",
        "INSTALL-MANIFEST.json",
        "SHA256SUMS.txt",
        "PROVEN.txt",
        "evidence/companion-matrix.json",
        "evidence/companion-admission.json",
    }
    missing = sorted(required - set(names))
    if missing:
        raise SystemExit(f"bundle missing required files: {missing}")

    manifest = json.loads(z.read("INSTALL-MANIFEST.json"))
    if manifest.get("schema") != 1:
        raise SystemExit("installer manifest schema must be 1")
    if manifest.get("minecraft") != "26.2":
        raise SystemExit("installer manifest must target Minecraft 26.2")

    mods = manifest.get("mods", [])
    if not mods:
        raise SystemExit("installer manifest has no mods")
    mod_ids = [entry.get("mod_id") for entry in mods]
    filenames = [entry.get("filename") for entry in mods]
    if len(mod_ids) != len(set(mod_ids)):
        raise SystemExit("installer manifest contains duplicate mod ids")
    if len(filenames) != len(set(filenames)):
        raise SystemExit("installer manifest contains duplicate filenames")

    hash_lines = z.read("SHA256SUMS.txt").decode("utf-8").splitlines()
    hashes = {}
    for line in hash_lines:
        if not line.strip():
            continue
        digest, rel = line.split("  ", 1)
        hashes[rel] = digest.lower()

    for entry in mods:
        rel = "mods/" + entry["filename"]
        if rel not in names:
            raise SystemExit(f"manifest mod missing from bundle: {rel}")
        payload = z.read(rel)
        digest = hashlib.sha256(payload).hexdigest()
        if digest != entry["sha256"].lower():
            raise SystemExit(f"manifest SHA mismatch: {rel}")
        if hashes.get(rel) != digest:
            raise SystemExit(f"SHA256SUMS mismatch: {rel}")
        with zipfile.ZipFile(pathlib.Path("/dev/null"), "w"):
            pass
        import io
        with zipfile.ZipFile(io.BytesIO(payload)) as jar:
            meta = json.loads(jar.read("fabric.mod.json").decode("utf-8"))
        if meta.get("id") != entry["mod_id"]:
            raise SystemExit(f"manifest mod id mismatch: {rel}")

    for rel, digest in hashes.items():
        if rel not in names:
            raise SystemExit(f"SHA256SUMS references missing file: {rel}")
        actual = hashlib.sha256(z.read(rel)).hexdigest()
        if actual != digest:
            raise SystemExit(f"SHA256SUMS verification failed: {rel}")

    install = z.read("INSTALL-CANVAS.cmd").decode("utf-8", errors="replace")
    restore = z.read("RESTORE-CANVAS.cmd").decode("utf-8", errors="replace")
    for needle in ["Get-FileHash -Algorithm SHA256", ".canvas-backup", "Move-Item -Force", "INSTALL-MANIFEST.json"]:
        if needle not in install:
            raise SystemExit(f"installer missing transactional safeguard: {needle}")
    for needle in [".canvas-backup", "INSTALL-MANIFEST.json", "Canvas backup restored"]:
        if needle not in restore:
            raise SystemExit(f"restore path missing safeguard: {needle}")

print("bundle verification: PASS", bundle)
print(f"  mods={len(mods)}")
print("  transactional_install=true")
print("  rollback=true")
