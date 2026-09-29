#!/usr/bin/env python3
from __future__ import annotations

import pathlib
import re
import sys

log = pathlib.Path("run/logs/latest.log")
if not log.exists():
    raise SystemExit("production latest.log missing")

text = log.read_text(encoding="utf-8", errors="replace")
lines = text.splitlines()

# CI/offline-client noise that is expected on GitHub-hosted runners.
allowed_error_patterns = [
    r"Failed to fetch user properties",
    r"Failed to fetch Realms feature flags",
    r"Couldn't connect to realms",
    r"Failed to retrieve profile key pair",
    r"Illegal option value 0 for Anisotropic Filtering",
    r"X11: Standard cursor shape unavailable",
]
allowed_warn_patterns = [
    r"Reference map '.*' .* could not be read",
    r"Error loading class: .*ClassNotFoundException.*inventive_inventory",
    r"Error loading class: .*ClassNotFoundException.*Inventorio",
    r"Error loading class: .*ClassNotFoundException.*trashslot",
]

fatal_patterns = [
    r"Minecraft has crashed!",
    r"MixinTransformerError",
    r"Could not execute entrypoint stage",
    r"Mod resolution encountered an incompatible mod set",
    r"Exception in server tick loop",
    r"NoClassDefFoundError",
    r"NoSuchMethodError",
    r"VerifyError",
    r"LinkageError",
]

fatals = [line for line in lines if any(re.search(p, line, re.I) for p in fatal_patterns)]
if fatals:
    print("fatal production-client runtime evidence:")
    print("\n".join(fatals[:50]))
    raise SystemExit(1)

unexpected_errors = []
for line in lines:
    if "/ERROR]" not in line and "[ERROR]" not in line:
        continue
    if any(re.search(p, line, re.I) for p in allowed_error_patterns):
        continue
    unexpected_errors.append(line)

unexpected_warns = []
for line in lines:
    if "/WARN]" not in line and "[WARN]" not in line:
        continue
    if any(re.search(p, line, re.I) for p in allowed_warn_patterns):
        continue
    # "Can't keep up" in ephemeral CI GameTest is not a production client safety
    # issue and appears in the separate client-gametest log, not latest.log.
    unexpected_warns.append(line)

if unexpected_errors:
    print("unexpected ERROR lines in production latest.log:")
    print("\n".join(unexpected_errors[:80]))
    raise SystemExit(1)

# Warnings are reported as evidence but do not fail until explicitly classified as
# harmful; this prevents known third-party compatibility noise from hiding.
print("production log health: PASS")
print(f"  unexpected_warnings={len(unexpected_warns)}")
for line in unexpected_warns[:40]:
    print("  WARN-EVIDENCE:", line)
