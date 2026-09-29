#!/usr/bin/env python3
import json, sys, zipfile
from pathlib import Path

jars=[Path(p) for p in sys.argv[1:] if not p.endswith("-sources.jar")]
if len(jars) != 1:
    raise SystemExit(f"expected exactly one runtime jar, got {jars}")

jar=jars[0]
with zipfile.ZipFile(jar) as z:
    meta=json.loads(z.read("fabric.mod.json"))
    names=set(z.namelist())

assert meta["id"] == "canvas"
assert meta["depends"]["minecraft"] == "~26.2"
assert "0.19.3" in meta["depends"]["fabricloader"]
assert "0.157.0+26.2" in meta["depends"]["fabric-api"]
assert any(n.endswith("CanvasMod.class") for n in names)
assert any(n.endswith("CanvasClient.class") for n in names)
assert any(n.endswith("CanvasHomeRuntime.class") for n in names)
assert any(n.endswith("CanvasFamiliarityClient.class") for n in names)
print("artifact verification: PASS", jar)
