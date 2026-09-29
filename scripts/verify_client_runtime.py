#!/usr/bin/env python3
from __future__ import annotations

import pathlib
import sys

root = pathlib.Path(".")
screens = sorted(root.glob("ci-evidence/client/*.png"))
if len(screens) < 2:
    raise SystemExit(f"expected at least two client screenshots, found {len(screens)}")

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

forbidden = [
    "File canvas:sounds/cues/coming_home.ogg does not exist",
    "File canvas:sounds/presence/hearth_air_v0.ogg does not exist",
    "File canvas:sounds/presence/harbor_air_v0.ogg does not exist",
    "File canvas:sounds/presence/void_stillness_v0.ogg does not exist",
]
present = [needle for needle in forbidden if needle in text]
if present:
    raise SystemExit("Canvas audio resource failures detected: " + ", ".join(present))

print(f"client evidence verification: PASS ({len(screens)} screenshots)")
