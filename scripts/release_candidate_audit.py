#!/usr/bin/env python3
from __future__ import annotations

import argparse
import hashlib
import json
import pathlib
import subprocess
import sys
import zipfile

ROOT = pathlib.Path(".").resolve()


def sha256_bytes(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def sha256_file(path: pathlib.Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def fail(message: str):
    raise SystemExit("release candidate audit FAILED: " + message)


parser = argparse.ArgumentParser()
parser.add_argument("--jar", required=True)
parser.add_argument("--bundle", required=True)
parser.add_argument("--admission", required=True)
parser.add_argument("--receipt", required=True)
args = parser.parse_args()

jar = pathlib.Path(args.jar)
bundle = pathlib.Path(args.bundle)
admission_path = pathlib.Path(args.admission)
receipt_path = pathlib.Path(args.receipt)

for path in [jar, bundle, admission_path]:
    if not path.exists():
        fail(f"required input missing: {path}")

queue = (ROOT / "AUTOMATION_QUEUE.md").read_text(encoding="utf-8")
if "- [ ]" in queue:
    fail("AUTOMATION_QUEUE.md still contains incomplete items")

properties = {}
for line in (ROOT / "gradle.properties").read_text(encoding="utf-8").splitlines():
    if "=" in line and not line.lstrip().startswith("#"):
        key, value = line.split("=", 1)
        properties[key.strip()] = value.strip()

version = properties.get("mod_version", "")
if not version.startswith("0.2.1-rc."):
    fail(f"release audit requires rc version, got {version!r}")
if properties.get("minecraft_version") != "26.2":
    fail("release audit requires Minecraft 26.2")

# Re-run source-level gates independently at the final checkpoint.
for command in [
    [sys.executable, "scripts/verify_invariants.py"],
    [sys.executable, "scripts/verify_compatibility_matrix.py"],
]:
    proc = subprocess.run(command, cwd=ROOT, capture_output=True, text=True)
    if proc.returncode != 0:
        fail(f"{' '.join(command)} failed:\n{proc.stdout}\n{proc.stderr}")

with zipfile.ZipFile(jar) as runtime:
    names = set(runtime.namelist())
    meta = json.loads(runtime.read("fabric.mod.json"))
    if meta.get("id") != "cozycanvas":
        fail("runtime JAR Fabric id is not cozycanvas")
    if str(meta.get("version")) != version:
        fail(f"runtime JAR version {meta.get('version')!r} != gradle version {version!r}")
    if meta.get("depends", {}).get("minecraft") != "~26.2":
        fail("runtime JAR Minecraft dependency is not ~26.2")
    required_runtime = [
        "CanvasMod.class",
        "CanvasClient.class",
        "CanvasFeatureConfig.class",
        "LowEndPerformanceBudgetPolicy.class",
        "P3MultiplayerScenarioPolicy.class",
        "WorldMemoryScopePolicy.class",
    ]
    for suffix in required_runtime:
        if not any(name.endswith(suffix) for name in names):
            fail(f"runtime JAR missing {suffix}")

admission = json.loads(admission_path.read_text(encoding="utf-8"))
active_entries = admission.get("active_entries", [])
active_filenames = {entry["filename"] for entry in active_entries}
if not active_filenames:
    fail("admission report has no active companion entries")

checkout_matrix = json.loads((ROOT / "compatibility/companion-matrix.json").read_text(encoding="utf-8"))

with zipfile.ZipFile(bundle) as proven:
    names = proven.namelist()
    if len(names) != len(set(names)):
        fail("proven bundle contains duplicate paths")
    required = {
        "INSTALL-CANVAS.cmd",
        "RESTORE-CANVAS.cmd",
        "INSTALL-MANIFEST.json",
        "SHA256SUMS.txt",
        "PROVEN.txt",
        "evidence/companion-matrix.json",
        "evidence/companion-admission.json",
    }
    missing = required - set(names)
    if missing:
        fail(f"proven bundle missing: {sorted(missing)}")

    manifest = json.loads(proven.read("INSTALL-MANIFEST.json"))
    packaged_mods = manifest.get("mods", [])
    packaged_filenames = {entry["filename"] for entry in packaged_mods}
    physical_mod_filenames = {
        pathlib.PurePosixPath(name).name
        for name in names
        if name.startswith("mods/") and name.lower().endswith(".jar")
    }
    if physical_mod_filenames != packaged_filenames:
        fail(
            "physical bundled JAR set differs from installer manifest: "
            f"physical={sorted(physical_mod_filenames)} manifest={sorted(packaged_filenames)}"
        )
    canvas_entries = [entry for entry in packaged_mods if entry.get("mod_id") == "cozycanvas"]
    if len(canvas_entries) != 1:
        fail(f"expected exactly one Canvas manifest entry, got {len(canvas_entries)}")

    canvas_rel = "mods/" + canvas_entries[0]["filename"]
    if sha256_bytes(proven.read(canvas_rel)) != sha256_file(jar):
        fail("bundled Canvas JAR does not match the validated runtime JAR")

    expected_packaged = set(active_filenames) | {canvas_entries[0]["filename"]}
    if packaged_filenames != expected_packaged:
        fail(
            "packaged mod set differs from validated admission set: "
            f"packaged={sorted(packaged_filenames)} expected={sorted(expected_packaged)}"
        )

    embedded_admission = json.loads(proven.read("evidence/companion-admission.json"))
    if embedded_admission != admission:
        fail("embedded companion admission report differs from validated report")

    embedded_matrix = json.loads(proven.read("evidence/companion-matrix.json"))
    if embedded_matrix != checkout_matrix:
        fail("embedded compatibility matrix differs from checked-in matrix")

    install = proven.read("INSTALL-CANVAS.cmd").decode("utf-8", errors="replace")
    restore = proven.read("RESTORE-CANVAS.cmd").decode("utf-8", errors="replace")
    for needle in [
        "[Security.Cryptography.SHA256]::Create()",
        ".canvas-backup",
        "Move-Item -Force",
        "INSTALL-MANIFEST.json",
    ]:
        if needle not in install:
            fail(f"installer missing transactional safeguard: {needle}")
    for needle in [
        ".canvas-backup",
        "INSTALL-MANIFEST.json",
        "Canvas backup restored",
        "-notlike '*-restored'",
    ]:
        if needle not in restore:
            fail(f"restore path missing safeguard: {needle}")

    hashes = {}
    for line in proven.read("SHA256SUMS.txt").decode("utf-8").splitlines():
        if not line.strip():
            continue
        digest, rel = line.split("  ", 1)
        hashes[rel] = digest.lower()
    expected_hashed = set(names) - {"SHA256SUMS.txt"}
    if set(hashes) != expected_hashed:
        fail(
            "SHA256SUMS coverage mismatch: "
            f"missing={sorted(expected_hashed - set(hashes))} "
            f"extra={sorted(set(hashes) - expected_hashed)}"
        )
    for rel, expected in hashes.items():
        if rel not in names:
            fail(f"SHA256SUMS references missing path: {rel}")
        if sha256_bytes(proven.read(rel)) != expected:
            fail(f"SHA256 mismatch in proven bundle: {rel}")

receipt = {
    "schema": 1,
    "release_version": version,
    "minecraft": "26.2",
    "runtime_jar": {
        "path": str(jar),
        "sha256": sha256_file(jar),
    },
    "proven_bundle": {
        "path": str(bundle),
        "sha256": sha256_file(bundle),
    },
    "active_companion_roots": admission.get("admitted_roots", []),
    "active_companion_jars": sorted(active_filenames),
    "quarantined_companions": admission.get("quarantined_roots", []),
    "checks": {
        "queue_complete": True,
        "source_invariants": True,
        "compatibility_contract": True,
        "runtime_artifact": True,
        "bundle_integrity": True,
        "validated_mod_set_exact": True,
        "embedded_evidence_exact": True,
    },
}
receipt_path.parent.mkdir(parents=True, exist_ok=True)
receipt_path.write_text(json.dumps(receipt, indent=2, sort_keys=True) + "\n", encoding="utf-8")

print("release candidate audit: PASS")
print("  version=", version)
print("  runtime_sha256=", receipt["runtime_jar"]["sha256"])
print("  bundle_sha256=", receipt["proven_bundle"]["sha256"])
print("  receipt=", receipt_path)
