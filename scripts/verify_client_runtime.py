#!/usr/bin/env python3
from __future__ import annotations

import hashlib
import json
import pathlib
from PIL import Image

root = pathlib.Path(".")
screens = sorted(root.glob("ci-evidence/client/*.png"))
if len(screens) < 2:
    raise SystemExit(f"expected at least two client screenshots, found {len(screens)}")

digests = [hashlib.sha256(path.read_bytes()).hexdigest() for path in screens]
if len(set(digests)) < 2:
    raise SystemExit("before/after FEEL screenshots are byte-identical")

before = Image.open(screens[0]).convert("RGB")
after = Image.open(screens[1]).convert("RGB")
if before.size != after.size:
    raise SystemExit(f"screenshot dimensions differ unexpectedly: {before.size} vs {after.size}")

a = list(before.getdata())
b = list(after.getdata())
total_abs = 0
changed = 0
for pa, pb in zip(a, b):
    delta = abs(pa[0] - pb[0]) + abs(pa[1] - pb[1]) + abs(pa[2] - pb[2])
    total_abs += delta
    if delta:
        changed += 1

pixels = max(1, len(a))
mean_channel_delta = total_abs / (pixels * 3)
changed_fraction = changed / pixels
if mean_channel_delta < 5.0:
    raise SystemExit(f"visual FEEL delta too weak: mean_channel_delta={mean_channel_delta:.3f}")
if changed_fraction < 0.25:
    raise SystemExit(f"visual FEEL coverage too small: changed_fraction={changed_fraction:.3%}")

log = pathlib.Path("run/logs/latest.log")
if not log.exists():
    raise SystemExit("production client latest.log was not produced")
text = log.read_text(encoding="utf-8", errors="replace")
text_lower = text.lower()

required = [
    "canvas 0.2.0-alpha.8",
    "Reloading ResourceManager:",
    "Sound engine started",
]
missing = [needle for needle in required if needle.lower() not in text_lower]
if missing:
    raise SystemExit("production client evidence missing: " + ", ".join(missing))

admission_path = pathlib.Path("ci-evidence/companion-admission.json")
if not admission_path.exists():
    raise SystemExit("companion admission report missing")
admission = json.loads(admission_path.read_text(encoding="utf-8"))

missing_mods = []
for entry in admission["active_entries"]:
    mod_id = entry.get("mod_id")
    mod_version = entry.get("mod_version")
    if not mod_id:
        continue
    needle = f"- {mod_id} {mod_version}".lower()
    if needle not in text_lower:
        missing_mods.append(f"{entry['slug']} => {mod_id} {mod_version}")
if missing_mods:
    raise SystemExit("resolved companion mods not loaded:\n  " + "\n  ".join(missing_mods))

game_test_console = pathlib.Path("ci-evidence/client/client-gametest-console.log")
if not game_test_console.exists():
    raise SystemExit("client GameTest console evidence was not preserved")
game_test_text = game_test_console.read_text(encoding="utf-8", errors="replace")
if "CANVAS_CI_VISUAL_ACTIVE" not in game_test_text:
    raise SystemExit("client GameTest never activated Canvas visual FEEL state")

forbidden = [
    "File canvas:sounds/cues/coming_home.ogg does not exist",
    "File canvas:sounds/cues/familiar_face.ogg does not exist",
    "File canvas:sounds/presence/hearth_air_v0.ogg does not exist",
    "File canvas:sounds/presence/harbor_air_v0.ogg does not exist",
    "File canvas:sounds/presence/void_stillness_v0.ogg does not exist",
]
present = [needle for needle in forbidden if needle in text]
if present:
    raise SystemExit("Canvas audio resource failures detected: " + ", ".join(present))

fatal_needles = [
    "mod resolution encountered an incompatible mod set",
    "could not execute entrypoint stage",
    "exception in server tick loop",
]
fatals = [needle for needle in fatal_needles if needle in text_lower]
if fatals:
    raise SystemExit("production client fatal runtime evidence: " + ", ".join(fatals))

print("client evidence verification: PASS")
print(f"  companions_loaded={len(admission['active_entries'])}")
print(f"  mean_channel_delta={mean_channel_delta:.3f}")
print(f"  changed_fraction={changed_fraction:.3%}")
for path, digest in zip(screens, digests):
    print(f"  {path.name} sha256={digest}")
