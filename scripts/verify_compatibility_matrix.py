#!/usr/bin/env python3
from __future__ import annotations

import ast
import json
import pathlib
import sys

ROOT = pathlib.Path(".")
MATRIX = ROOT / "compatibility" / "companion-matrix.json"
FETCH = ROOT / "scripts" / "fetch_companions.py"
ADMIT = ROOT / "scripts" / "admit_companions.py"


def assigned_literal(path: pathlib.Path, name: str):
    tree = ast.parse(path.read_text(encoding="utf-8"), filename=str(path))
    for node in tree.body:
        if isinstance(node, ast.Assign):
            for target in node.targets:
                if isinstance(target, ast.Name) and target.id == name:
                    return ast.literal_eval(node.value)
    raise SystemExit(f"{path}: missing literal assignment {name}")


matrix = json.loads(MATRIX.read_text(encoding="utf-8"))
if matrix.get("schema") != 1:
    raise SystemExit("compatibility matrix schema must be 1")
if matrix.get("minecraft") != "26.2" or matrix.get("loader") != "fabric":
    raise SystemExit("compatibility matrix must target Minecraft 26.2 Fabric")

entries = matrix.get("entries", [])
slugs = [entry["slug"] for entry in entries]
if len(slugs) != len(set(slugs)):
    raise SystemExit("compatibility matrix contains duplicate slugs")

required = {entry["slug"] for entry in entries if entry.get("policy") == "required"}
trial = {entry["slug"] for entry in entries if entry.get("policy") == "trial"}
if required | trial != set(slugs):
    raise SystemExit("every compatibility entry must be required or trial")

resolver_roots = set(assigned_literal(FETCH, "ROOT_PROJECTS"))
core_roots = set(assigned_literal(ADMIT, "CORE_ROOTS"))
optional_roots = set(assigned_literal(ADMIT, "OPTIONAL_ROOTS"))

if resolver_roots != set(slugs):
    raise SystemExit(
        f"compatibility matrix/resolver mismatch: matrix={sorted(slugs)} resolver={sorted(resolver_roots)}"
    )
if required != core_roots:
    raise SystemExit(
        f"compatibility required/core mismatch: matrix={sorted(required)} core={sorted(core_roots)}"
    )
if trial != optional_roots:
    raise SystemExit(
        f"compatibility trial/optional mismatch: matrix={sorted(trial)} optional={sorted(optional_roots)}"
    )

if len(sys.argv) > 1:
    report_path = pathlib.Path(sys.argv[1])
    report = json.loads(report_path.read_text(encoding="utf-8"))
    admitted = set(report.get("admitted_roots", []))
    quarantined = {item["slug"] for item in report.get("quarantined_roots", [])}

    missing_required = required - admitted
    if missing_required:
        raise SystemExit(f"required companions were not admitted: {sorted(missing_required)}")
    if required & quarantined:
        raise SystemExit(f"required companions were quarantined: {sorted(required & quarantined)}")

    accounted_trial = (admitted | quarantined) & trial
    if accounted_trial != trial:
        raise SystemExit(f"trial companions not fully accounted: {sorted(trial - accounted_trial)}")
    if admitted & quarantined:
        raise SystemExit(f"companions cannot be both admitted and quarantined: {sorted(admitted & quarantined)}")
    unknown = (admitted | quarantined) - set(slugs)
    if unknown:
        raise SystemExit(f"admission report contains unknown root companions: {sorted(unknown)}")

print("compatibility matrix verification: PASS")
print("  required:", ", ".join(sorted(required)))
print("  trial:", ", ".join(sorted(trial)))
