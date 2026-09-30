#!/usr/bin/env python3
import hashlib
import pathlib
import re
import sys
import zipfile

bundle = pathlib.Path(sys.argv[1])
with zipfile.ZipFile(bundle) as z:
    names = set(z.namelist())
    required = {"PROVEN.txt", "SHA256SUMS.txt", "INSTALL-CANVAS.cmd",
                "evidence/companion-matrix.json", "evidence/companion-admission.json"}
    if required - names:
        raise SystemExit("candidate bundle is incomplete")
    checked = set()
    for line in z.read("SHA256SUMS.txt").decode().splitlines():
        match = re.fullmatch(r"([0-9a-f]{64})  (.+)", line)
        if not match:
            raise SystemExit("invalid checksum manifest")
        expected, name = match.groups()
        if name not in names:
            raise SystemExit("checksummed payload missing")
        if hashlib.sha256(z.read(name)).hexdigest() != expected:
            raise SystemExit("checksum mismatch")
        checked.add(name)
    mods = {n for n in names if n.startswith("mods/") and n.endswith(".jar")}
    if not mods or not mods.issubset(checked):
        raise SystemExit("mod payload is not fully checksum-bound")
print("candidate bundle verification: PASS")
