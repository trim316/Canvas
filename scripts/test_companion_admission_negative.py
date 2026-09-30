#!/usr/bin/env python3
"""Adversarial contract tests for companion admission, using synthetic reports only."""
from __future__ import annotations

import copy
import json
import pathlib
import shutil
import subprocess
import sys
import tempfile

ROOT = pathlib.Path(__file__).resolve().parents[1]
SOURCE = [
    "compatibility/companion-matrix.json",
    "scripts/fetch_companions.py",
    "scripts/admit_companions.py",
    "scripts/verify_compatibility_matrix.py",
]


def main() -> None:
    with tempfile.TemporaryDirectory(prefix="canvas-companion-policy-") as temp:
        base = pathlib.Path(temp)
        for rel in SOURCE:
            source, dest = ROOT / rel, base / rel
            dest.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(source, dest)
        matrix = json.loads((base / SOURCE[0]).read_text(encoding="utf-8"))
        required = {e["slug"] for e in matrix["entries"] if e["policy"] == "required"}
        trial = {e["slug"] for e in matrix["entries"] if e["policy"] == "trial"}
        assert required and trial, "negative-case coverage requires required and trial companions"
        report = {
            "admitted_roots": sorted(required),
            "quarantined_roots": [
                {"slug": slug, "reason": "synthetic quarantined optional"}
                for slug in sorted(trial)
            ],
        }
        path = base / "admission.json"
        verifier = base / "scripts/verify_compatibility_matrix.py"

        def run(record: dict, passed: bool, case: str) -> None:
            path.write_text(json.dumps(record), encoding="utf-8")
            proc = subprocess.run(
                [sys.executable, str(verifier), str(path)],
                cwd=base, text=True, stdout=subprocess.PIPE,
                stderr=subprocess.STDOUT, timeout=15,
            )
            if (proc.returncode == 0) != passed:
                raise AssertionError(
                    f"{case} expected passed={passed}, exit={proc.returncode}: "
                    + proc.stdout
                )

        run(report, True, "valid required plus quarantined trial")
        first_required = sorted(required)[0]
        first_trial = sorted(trial)[0]

        missing = copy.deepcopy(report)
        missing["admitted_roots"].remove(first_required)
        run(missing, False, "missing required integration")

        invalid_quarantine = copy.deepcopy(report)
        invalid_quarantine["admitted_roots"].remove(first_required)
        invalid_quarantine["quarantined_roots"].append({"slug": first_required})
        run(invalid_quarantine, False, "quarantined required integration")

        unaccounted = copy.deepcopy(report)
        unaccounted["quarantined_roots"] = [
            x for x in unaccounted["quarantined_roots"] if x["slug"] != first_trial
        ]
        run(unaccounted, False, "unaccounted optional integration")

        conflicting = copy.deepcopy(report)
        conflicting["admitted_roots"].append(first_trial)
        run(conflicting, False, "simultaneously admitted and quarantined integration")

        unknown = copy.deepcopy(report)
        unknown["admitted_roots"].append("unexpected-unreviewed-mod")
        run(unknown, False, "unexpected external integration")

        matrix_path = base / SOURCE[0]
        altered = copy.deepcopy(matrix)
        altered["entries"][0]["slug"] = "substituted-resolver-root"
        matrix_path.write_text(json.dumps(altered), encoding="utf-8")
        run(report, False, "resolver/checked-in compatibility drift")
        print("companion admission negative regression: PASS (six rejections)")


if __name__ == "__main__":
    main()
