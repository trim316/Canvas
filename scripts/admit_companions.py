#!/usr/bin/env python3
from __future__ import annotations

import json
import os
import pathlib
import shutil
import subprocess
import sys

ROOT = pathlib.Path(".").resolve()
LOCK = ROOT / "ci-mods" / "companion-lock.json"
SOURCE = ROOT / "ci-mods"
ACTIVE = ROOT / "ci-mods-active"
REPORT = ROOT / "ci-evidence" / "companion-admission.json"

CORE_ROOTS = ["coolrain", "sound-physics-remastered"]
OPTIONAL_ROOTS = [
    "ambientsounds",
    "dcme-dynamic-contextual-music-engine",
    "sound",
]


def load_lock():
    if not LOCK.exists():
        raise SystemExit("companion lock missing")
    return json.loads(LOCK.read_text(encoding="utf-8"))


def closure(lock: dict, roots: list[str]) -> list[dict]:
    entries = {entry["slug"]: entry for entry in lock["entries"]}
    by_project = {entry["project_id"]: entry for entry in lock["entries"]}
    wanted_projects = set()

    for root in roots:
        if root not in entries:
            raise SystemExit(f"root companion missing from lock: {root}")
        wanted_projects.add(entries[root]["project_id"])

    changed = True
    while changed:
        changed = False
        for entry in lock["entries"]:
            reason = entry.get("reason", "")
            if not reason.startswith("required-by:"):
                continue
            parent_slug = reason.split(":", 1)[1]
            parent = entries.get(parent_slug)
            if parent and parent["project_id"] in wanted_projects and entry["project_id"] not in wanted_projects:
                wanted_projects.add(entry["project_id"])
                changed = True

    return [entry for entry in lock["entries"] if entry["project_id"] in wanted_projects]


def materialize(entries: list[dict]) -> None:
    if ACTIVE.exists():
        shutil.rmtree(ACTIVE)
    ACTIVE.mkdir(parents=True)
    for entry in entries:
        source = SOURCE / entry["filename"]
        if not source.exists():
            raise SystemExit(f"resolved JAR missing: {source}")
        shutil.copy2(source, ACTIVE / source.name)


def launch(label: str) -> tuple[bool, str]:
    env = os.environ.copy()
    env["CANVAS_COMPANION_DIR"] = str(ACTIVE)
    env["ALSOFT_DRIVERS"] = "null"
    log = ROOT / "ci-evidence" / f"companion-{label}.log"
    log.parent.mkdir(parents=True, exist_ok=True)
    cmd = [
        "gradle",
        "runProductionClientGameTest",
        "--no-daemon",
        "--no-configuration-cache",
        "--stacktrace",
    ]
    with log.open("w", encoding="utf-8", errors="replace") as out:
        proc = subprocess.run(cmd, cwd=ROOT, env=env, stdout=out, stderr=subprocess.STDOUT)
    return proc.returncode == 0, str(log.relative_to(ROOT))


def main() -> int:
    lock = load_lock()
    admitted = list(CORE_ROOTS)
    quarantined: list[dict] = []
    attempts: list[dict] = []

    core_entries = closure(lock, admitted)
    materialize(core_entries)
    ok, log = launch("core")
    attempts.append({"roots": admitted.copy(), "ok": ok, "log": log})
    if not ok:
        raise SystemExit("core companion stack failed production client launch")

    for candidate in OPTIONAL_ROOTS:
        trial = admitted + [candidate]
        trial_entries = closure(lock, trial)
        materialize(trial_entries)
        ok, log = launch(candidate.replace("/", "_"))
        attempts.append({"roots": trial.copy(), "ok": ok, "log": log})
        if ok:
            admitted.append(candidate)
            print(f"{candidate}: ADMITTED")
        else:
            quarantined.append({"slug": candidate, "reason": "production-client-incompatibility", "log": log})
            print(f"{candidate}: QUARANTINED")

    final_entries = closure(lock, admitted)
    materialize(final_entries)

    report = {
        "schema": 1,
        "admitted_roots": admitted,
        "quarantined_roots": quarantined,
        "active_entries": [
            {
                "slug": e["slug"],
                "title": e["title"],
                "version_number": e["version_number"],
                "filename": e["filename"],
                "mod_id": e.get("mod_id"),
                "mod_version": e.get("mod_version"),
            }
            for e in final_entries
        ],
        "attempts": attempts,
    }
    REPORT.parent.mkdir(parents=True, exist_ok=True)
    REPORT.write_text(json.dumps(report, indent=2, sort_keys=True) + "\n", encoding="utf-8")

    print("companion admission: PASS")
    print("  admitted:", ", ".join(admitted))
    if quarantined:
        print("  quarantined:", ", ".join(item["slug"] for item in quarantined))
    else:
        print("  quarantined: none")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
