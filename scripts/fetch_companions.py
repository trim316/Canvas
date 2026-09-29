#!/usr/bin/env python3
import hashlib
import json
import pathlib
import urllib.request

VERSIONS = {
    "cool-rain": "zc88gNk3",
    "sound-physics-remastered": "d8iioMMp",
}

OUT = pathlib.Path("ci-mods")
OUT.mkdir(parents=True, exist_ok=True)

for name, version_id in VERSIONS.items():
    url = f"https://api.modrinth.com/v2/version/{version_id}"
    req = urllib.request.Request(url, headers={"User-Agent": "Canvas-CI/0.2"})
    with urllib.request.urlopen(req, timeout=45) as response:
        meta = json.loads(response.read().decode("utf-8"))

    if "26.2" not in meta.get("game_versions", []):
        raise SystemExit(f"{name}: pinned version does not support Minecraft 26.2")
    if "fabric" not in meta.get("loaders", []):
        raise SystemExit(f"{name}: pinned version is not Fabric")

    files = meta.get("files") or []
    selected = next((f for f in files if f.get("primary")), files[0])
    target = OUT / selected["filename"]

    req = urllib.request.Request(selected["url"], headers={"User-Agent": "Canvas-CI/0.2"})
    with urllib.request.urlopen(req, timeout=90) as response, target.open("wb") as out:
        out.write(response.read())

    expected = selected.get("hashes", {}).get("sha512")
    if expected:
        actual = hashlib.sha512(target.read_bytes()).hexdigest()
        if actual.lower() != expected.lower():
            raise SystemExit(f"{name}: SHA-512 mismatch")

    print(f"{name}: PASS {target.name}")
