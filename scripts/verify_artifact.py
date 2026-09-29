#!/usr/bin/env python3
import json
import sys
import zipfile
from pathlib import Path

jars = [Path(p) for p in sys.argv[1:] if not p.endswith("-sources.jar")]
if len(jars) != 1:
    raise SystemExit(f"expected exactly one runtime jar, got {jars}")

jar = jars[0]
with zipfile.ZipFile(jar) as z:
    meta = json.loads(z.read("fabric.mod.json"))
    names = set(z.namelist())

assert meta["id"] == "canvas"
assert meta["depends"]["minecraft"] == "~26.2"
assert "0.19.3" in meta["depends"]["fabricloader"]
assert "0.157.0+26.2" in meta["depends"]["fabric-api"]

required_classes = [
    "CanvasMod.class",
    "CanvasFeelProfile.class",
    "HomecomingPolicy.class",
    "RareSurprisePolicy.class",
    "WeatherCharacterPolicy.class",
    "VillageLifePolicy.class",
    "ContextualMusicPolicy.class",
    "HomeStatePayload.class",
    "CanvasClient.class",
    "CanvasHomeRuntime.class",
    "HomeEvidencePolicy.class",
    "HomeEvidenceDetector.class",
    "HomeRecognitionAccumulator.class",
    "FamiliarityPolicy.class",
    "CanvasFamiliarityClient.class",
    "CanvasFeelClient.class",
    "CanvasExperienceDirector.class",
    "CanvasVillageLifeClient.class",
]
for required in required_classes:
    assert any(name.endswith(required) for name in names), required

required_resources = [
    "assets/canvas/sounds.json",
    "assets/canvas/lang/en_us.json",
    "assets/canvas/sounds/cues/coming_home.ogg",
    "assets/canvas/sounds/cues/coming_home_familiar.ogg",
    "assets/canvas/sounds/cues/coming_home_village.ogg",
    "assets/canvas/sounds/cues/coming_home_lived_in.ogg",
    "assets/canvas/sounds/cues/rare_storm_break.ogg",
    "assets/canvas/sounds/cues/rare_golden_hush.ogg",
    "assets/canvas/sounds/cues/rare_starlit_stillness.ogg",
    "assets/canvas/sounds/cues/calm_after_storm.ogg",
    "assets/canvas/sounds/presence/rain_on_roof.ogg",
    "assets/canvas/sounds/presence/thunder_shelter.ogg",
    "assets/canvas/sounds/cues/familiar_face.ogg",
    "assets/canvas/sounds/cues/home_shift.ogg",
    "assets/canvas/sounds/music/coming_home.ogg",
    "assets/canvas/sounds/music/coming_home_storm.ogg",
    "assets/canvas/sounds/music/village_wake.ogg",
    "assets/canvas/sounds/music/village_wind_down.ogg",
    "assets/canvas/sounds/music/community_gathering.ogg",
    "assets/canvas/sounds/presence/home_morning.ogg",
    "assets/canvas/sounds/presence/home_day.ogg",
    "assets/canvas/sounds/presence/home_evening.ogg",
    "assets/canvas/sounds/presence/home_night.ogg",
    "assets/canvas/sounds/presence/home_storm.ogg",
    "assets/canvas/sounds/presence/harbor_air_v0.ogg",
    "assets/canvas/sounds/presence/void_stillness_v0.ogg",
]
for required in required_resources:
    assert required in names, required

print("artifact verification: PASS", jar)
