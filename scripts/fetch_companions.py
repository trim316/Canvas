#!/usr/bin/env python3
from __future__ import annotations

import hashlib
import json
import pathlib
import urllib.parse
import urllib.request
import zipfile
from datetime import datetime

GAME_VERSION = "26.2"
LOADER = "fabric"
ROOT_PROJECTS = [
    "coolrain",
    "sound-physics-remastered",
    "ambientsounds",
    "dcme-dynamic-contextual-music-engine",
    "sound",
]

OUT = pathlib.Path("ci-mods")
OUT.mkdir(parents=True, exist_ok=True)
for old in OUT.glob("*.jar"):
    old.unlink()
lock_path = OUT / "companion-lock.json"
if lock_path.exists():
    lock_path.unlink()

HEADERS = {"User-Agent": "Canvas-CI/0.3 (+https://github.com/trim316/Canvas)"}


def get_json(url: str):
    request = urllib.request.Request(url, headers=HEADERS)
    with urllib.request.urlopen(request, timeout=60) as response:
        return json.loads(response.read().decode("utf-8"))


def version_meta(version_id: str):
    return get_json(f"https://api.modrinth.com/v2/version/{version_id}")


def project_meta(project: str):
    return get_json(f"https://api.modrinth.com/v2/project/{project}")


def compatible_versions(project: str):
    query = urllib.parse.urlencode({
        "loaders": json.dumps([LOADER]),
        "game_versions": json.dumps([GAME_VERSION]),
    })
    return get_json(f"https://api.modrinth.com/v2/project/{project}/version?{query}")


def choose_version(project: str):
    versions = compatible_versions(project)
    if not versions:
        raise SystemExit(f"{project}: no {LOADER} version for Minecraft {GAME_VERSION}")

    rank = {"release": 0, "beta": 1, "alpha": 2}
    versions.sort(
        key=lambda item: (
            rank.get(item.get("version_type", "alpha"), 9),
            -datetime.fromisoformat(item["date_published"].replace("Z", "+00:00")).timestamp(),
        )
    )
    return versions[0]


def download_file(meta: dict) -> pathlib.Path:
    files = [f for f in meta.get("files", []) if not f["filename"].lower().endswith("-sources.jar")]
    if not files:
        raise SystemExit(f"{meta.get('name', meta.get('id'))}: no runtime file")
    selected = next((f for f in files if f.get("primary")), files[0])
    target = OUT / selected["filename"]

    request = urllib.request.Request(selected["url"], headers=HEADERS)
    with urllib.request.urlopen(request, timeout=120) as response, target.open("wb") as out:
        while True:
            block = response.read(1024 * 1024)
            if not block:
                break
            out.write(block)

    expected = selected.get("hashes", {}).get("sha512")
    actual512 = hashlib.sha512(target.read_bytes()).hexdigest()
    if expected and actual512.lower() != expected.lower():
        raise SystemExit(f"{target.name}: SHA-512 mismatch")
    return target


def read_fabric_metadata(path: pathlib.Path):
    try:
        with zipfile.ZipFile(path) as archive:
            return json.loads(archive.read("fabric.mod.json").decode("utf-8"))
    except (KeyError, json.JSONDecodeError, zipfile.BadZipFile):
        return None


resolved_projects: set[str] = set()
resolved_versions: set[str] = set()
queue: list[tuple[str, str | None, str]] = [(project, None, "root") for project in ROOT_PROJECTS]
lock_entries: list[dict] = []

while queue:
    project_ref, pinned_version, reason = queue.pop(0)
    pmeta = project_meta(project_ref)
    project_id = pmeta["id"]
    if project_id in resolved_projects:
        continue

    meta = version_meta(pinned_version) if pinned_version else choose_version(project_id)
    if GAME_VERSION not in meta.get("game_versions", []):
        raise SystemExit(f"{pmeta['slug']}: selected version does not support {GAME_VERSION}")
    if LOADER not in meta.get("loaders", []):
        raise SystemExit(f"{pmeta['slug']}: selected version is not {LOADER}")

    path = download_file(meta)
    fabric = read_fabric_metadata(path)
    mod_id = fabric.get("id") if fabric else None
    mod_version = str(fabric.get("version")) if fabric else meta.get("version_number")

    entry = {
        "project_id": project_id,
        "slug": pmeta["slug"],
        "title": pmeta["title"],
        "version_id": meta["id"],
        "version_number": meta["version_number"],
        "version_type": meta.get("version_type"),
        "reason": reason,
        "filename": path.name,
        "sha256": hashlib.sha256(path.read_bytes()).hexdigest(),
        "sha512": hashlib.sha512(path.read_bytes()).hexdigest(),
        "mod_id": mod_id,
        "mod_version": mod_version,
    }
    lock_entries.append(entry)
    resolved_projects.add(project_id)
    resolved_versions.add(meta["id"])
    print(
        f"{entry['title']}: PASS {entry['version_number']} "
        f"id={entry['mod_id']} file={entry['filename']}"
    )

    for dependency in meta.get("dependencies", []):
        if dependency.get("dependency_type") != "required":
            continue
        dependency_project = dependency.get("project_id")
        dependency_version = dependency.get("version_id")
        if not dependency_project and dependency_version:
            dep_meta = version_meta(dependency_version)
            dependency_project = dep_meta["project_id"]
        if not dependency_project:
            raise SystemExit(f"{pmeta['slug']}: required dependency without project id")
        if dependency_project in resolved_projects:
            continue
        queue.append((dependency_project, dependency_version, f"required-by:{pmeta['slug']}"))

root_project_ids = {project_meta(project)["id"] for project in ROOT_PROJECTS}
resolved_root_ids = {entry["project_id"] for entry in lock_entries if entry["reason"] == "root"}
if resolved_root_ids != root_project_ids:
    missing = root_project_ids - resolved_root_ids
    raise SystemExit(f"not all root companions resolved: {sorted(missing)}")

lock = {
    "schema": 1,
    "minecraft": GAME_VERSION,
    "loader": LOADER,
    "roots": ROOT_PROJECTS,
    "entries": sorted(lock_entries, key=lambda item: (item["reason"] != "root", item["slug"])),
}
lock_path.write_text(json.dumps(lock, indent=2, sort_keys=True) + "\n", encoding="utf-8")

print(f"companion resolver: PASS ({len(lock_entries)} JARs)")
print(f"lock: {lock_path}")
