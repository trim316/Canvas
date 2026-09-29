#!/usr/bin/env python3
from __future__ import annotations

import hashlib
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
required = [
    "canvas 0.2.0-alpha.4",
    "coolrain",
    "sound_physics_remastered",
    "Reloading ResourceManager:",
    "Sound engine started",
]
missing = [needle for needle in required if needle not in text]
if missing:
    raise SystemExit("production client evidence missing: " + ", ".join(missing))

game_test_console = pathlib.Path("ci-evidence/client/client-gametest-console.log")
if not game_test_console.exists():
    raise SystemExit("client GameTest console evidence was not preserved")
game_test_text = game_test_console.read_text(encoding="utf-8", errors="replace")
if "CANVAS_CI_VISUAL_ACTIVE" not in game_test_text:
    raise SystemExit("client GameTest never activated Canvas visual FEEL state")

forbidden = [
    "File canvas:sounds/cues/coming_home.ogg does not exist",
    "File canvas:sounds/presence/hearth_air_v0.ogg does not exist",
    "File canvas:sounds/presence/harbor_air_v0.ogg does not exist",
    "File canvas:sounds/presence/void_stillness_v0.ogg does not exist",
]
present = [needle for needle in forbidden if needle in text]
if present:
    raise SystemExit("Canvas audio resource failures detected: " + ", ".join(present))

print("client evidence verification: PASS")
print(f"  mean_channel_delta={mean_channel_delta:.3f}")
print(f"  changed_fraction={changed_fraction:.3%}")
for path, digest in zip(screens, digests):
    print(f"  {path.name} sha256={digest}")
