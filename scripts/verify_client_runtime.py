#!/usr/bin/env python3
from __future__ import annotations

import hashlib
import pathlib

root = pathlib.Path(".")
screens = sorted(root.glob("ci-evidence/client/*.png"))
if len(screens) < 2:
    raise SystemExit(f"expected at least two client screenshots, found {len(screens)}")

digests = [hashlib.sha256(path.read_bytes()).hexdigest() for path in screens]
if len(set(digests)) < 2:
    raise SystemExit("before/after FEEL screenshots are byte-identical")

log = pathlib.Path("run/logs/latest.log")
if not log.exists():
    raise SystemExit("production client latest.log was not produced")

text = log.read_text(encoding="utf-8", errors="replace")
required = [
    "canvas 0.2.0-alpha.2",
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
for path, digest in zip(screens, digests):
    print(f"  {path.name} sha256={digest}")
