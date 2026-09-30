#!/usr/bin/env python3
"""Non-mutating hourly Canvas continuity audit (GitHub-hosted runner only).

Checks actual source, test/PR activity and durable READY queue; never touches
local Modrinth or Minecraft and never represents a CI check as a coding agent.
"""
from __future__ import annotations

import datetime as dt
import json
import os
import pathlib
import subprocess
import urllib.error
import urllib.request

ROOT = pathlib.Path(__file__).resolve().parents[2]
OUT = pathlib.Path("cloud-continuity-evidence")
OUT.mkdir(parents=True, exist_ok=True)
API = "https://api.github.com/repos/trim316/Canvas"
TOKEN = os.environ.get("GITHUB_TOKEN", "")


def api(path: str):
    headers = {"Accept": "application/vnd.github+json", "User-Agent": "canvas-continuity-audit"}
    if TOKEN:
        headers["Authorization"] = "Bearer " + TOKEN
    request = urllib.request.Request(API + path, headers=headers)
    with urllib.request.urlopen(request, timeout=20) as response:
        return json.load(response)


def age_minutes(stamp: str, now: dt.datetime) -> float:
    created = dt.datetime.fromisoformat(stamp.replace("Z", "+00:00"))
    return round((now - created).total_seconds() / 60, 1)


def main() -> None:
    now = dt.datetime.now(dt.timezone.utc)
    report = {
        "schema": 1, "observed_at": now.isoformat(),
        "checked_sha": subprocess.check_output(
            ["git", "rev-parse", "HEAD"], cwd=ROOT, text=True
        ).strip(),
        "mode": "nonmutating_hosted_audit_not_autonomous_code_development",
        "queue": {}, "main": {}, "pull_requests": [],
        "active_runs": [], "warnings": [], "errors": [],
    }

    queue_path = ROOT / "POST_RC_QUEUE.md"
    if not queue_path.is_file():
        report["errors"].append("POST_RC_QUEUE.md unavailable on checked-out main")
    else:
        queue = queue_path.read_text(encoding="utf-8")
        actionable = [
            line.strip() for line in queue.splitlines()
            if line.lstrip().startswith("- [ ] READY:")
        ]
        deferred = [
            line.strip() for line in queue.splitlines()
            if line.lstrip().startswith("- [ ] EXTERNAL ACCEPTANCE:")
        ]
        report["queue"] = {
            "ready_count": len(actionable),
            "ready": actionable,
            "deferred_user_playtest_count": len(deferred),
        }
        if not actionable:
            report["warnings"].append(
                "No independently READY work; replenish queue before ending development session"
            )

    try:
        commit = api("/commits/main")
        report["main"] = {
            "sha": commit["sha"],
            "committed_at": commit["commit"]["committer"]["date"],
            "minutes_since_last_commit": age_minutes(
                commit["commit"]["committer"]["date"], now
            ),
        }
        prs = api("/pulls?state=open&per_page=30")
        for item in prs:
            report["pull_requests"].append({
                "number": item["number"], "title": item["title"],
                "head_sha": item["head"]["sha"],
                "updated_at": item["updated_at"],
            })
        runs = api("/actions/runs?per_page=20")["workflow_runs"]
        for item in runs:
            if item["name"] == "Canvas Hourly Cloud Continuity":
                continue
            if item["status"] != "completed":
                report["active_runs"].append({
                    "id": item["id"], "name": item["name"],
                    "branch": item["head_branch"], "status": item["status"],
                })
        # A stale main SHA is not a stall if work is active on a validated PR.
        active_prs = bool(report["pull_requests"])
        active_ci = bool(report["active_runs"])
        if report["main"]["minutes_since_last_commit"] > 120:
            if active_prs or active_ci:
                report["warnings"].append(
                    "Main older than two hours but PRs or CI active; inspect progress, not automatic stall"
                )
            elif report["queue"].get("ready_count"):
                report["warnings"].append(
                    "Potential stall: main older than two hours, READY work exists, no open PRs or active CI"
                )
    except (KeyError, ValueError, urllib.error.URLError) as exc:
        report["errors"].append(f"GitHub API continuity inspection failed: {exc}")

    report["verdict"] = "ERROR" if report["errors"] else (
        "ATTENTION" if report["warnings"] else "PASS"
    )
    (OUT / "continuity.json").write_text(
        json.dumps(report, indent=2, sort_keys=True) + "\n", encoding="utf-8"
    )
    print("Canvas hosted continuity audit:", report["verdict"])
    print("checked", report["checked_sha"])
    print("READY", report["queue"].get("ready_count", "unknown"))
    print("open PRs", len(report["pull_requests"]))
    print("active CI", len(report["active_runs"]))
    for warning in report["warnings"]:
        print("WARNING:", warning)
    for error in report["errors"]:
        print("ERROR:", error)
    if report["errors"]:
        raise SystemExit(1)


if __name__ == "__main__":
    main()
