#!/usr/bin/env python3
"""Validate an EXACT published Canvas bundle on a throwaway Windows Modrinth profile."""
from __future__ import annotations
import hashlib, json, os, pathlib, shutil, subprocess, sys, tempfile, zipfile

def sha256(path: pathlib.Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()

def invoke(cmd: pathlib.Path, appdata: pathlib.Path, expect: bool) -> str:
    env = os.environ.copy()
    env["APPDATA"] = str(appdata)
    p = subprocess.run(["cmd","/d","/c",str(cmd)],cwd=cmd.parent,env=env,
                       stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True,timeout=180)
    if (p.returncode == 0) != expect:
        raise AssertionError(f"{cmd.name} expect={expect} rc={p.returncode}\n{p.stdout}")
    return p.stdout

def jar_id(path: pathlib.Path) -> str:
    with zipfile.ZipFile(path) as z:
        return str(json.loads(z.read("fabric.mod.json"))["id"])

def main() -> None:
    if sys.platform != "win32": raise SystemExit("Windows runner required")
    if len(sys.argv) != 4:
        raise SystemExit("usage: release_windows_acceptance.py <bundle.zip> <audit.json> <expected-sha256>")
    bundle, audit = map(pathlib.Path, sys.argv[1:3])
    expected = sys.argv[3].strip().lower()
    assert sha256(bundle) == expected, "published ZIP checksum mismatch"
    receipt = json.loads(audit.read_text(encoding="utf-8"))
    assert receipt["proven_bundle"]["sha256"] == expected
    assert receipt["minecraft"] == "26.2"
    assert all(receipt["checks"].values())

    with tempfile.TemporaryDirectory(prefix="canvas-release-accept-") as td:
        root = pathlib.Path(td)
        stage = root / "release"
        with zipfile.ZipFile(bundle) as z: z.extractall(stage)
        manifest = json.loads((stage/"INSTALL-MANIFEST.json").read_text(encoding="utf-8"))
        mods_expected = {m["mod_id"]:m for m in manifest["mods"]}
        appdata = root/"AppData"
        mods = appdata/"ModrinthApp"/"profiles"/"Fabulously Optimized"/"mods"
        mods.mkdir(parents=True)

        # Seed an existing profile entry for every ID with deterministic bytes/JAR metadata.
        baseline = {}
        for i, mid in enumerate(sorted(mods_expected)):
            old = mods/f"preexisting-{i:03d}.jar"
            with zipfile.ZipFile(old,"w") as z:
                z.writestr("fabric.mod.json", json.dumps({"schemaVersion":1,"id":mid,"version":"acceptance-old"}))
                z.writestr("baseline.bin", f"original-{mid}".encode())
            baseline[mid]=(old.name,sha256(old))

        invoke(stage/"INSTALL-CANVAS.cmd",appdata,True)
        installed = {}
        for p in mods.glob("*.jar"):
            installed[jar_id(p)] = (p.name,sha256(p))
        assert set(installed)==set(mods_expected), (
            f"installed mod IDs differ from release manifest: {set(installed)^set(mods_expected)}")
        for mid,entry in mods_expected.items():
            assert installed[mid][0] == entry["filename"]
            assert installed[mid][1] == entry["sha256"]

        # Reinstall must stay transactional and produce another reversible snapshot.
        invoke(stage/"INSTALL-CANVAS.cmd",appdata,True)
        installed2={jar_id(p):(p.name,sha256(p)) for p in mods.glob("*.jar")}
        assert installed2==installed, "repeat install changed exact release files"

        # First restore returns the immediately previous (same release) snapshot.
        invoke(stage/"RESTORE-CANVAS.cmd",appdata,True)
        restored_once={jar_id(p):(p.name,sha256(p)) for p in mods.glob("*.jar")}
        assert restored_once==installed, "first restore did not reproduce prior installed release"

        # Second restore returns the original seeded profile.
        invoke(stage/"RESTORE-CANVAS.cmd",appdata,True)
        originals={jar_id(p):(p.name,sha256(p)) for p in mods.glob("*.jar")}
        assert set(originals)==set(baseline)
        for mid,(name,digest) in baseline.items():
            assert originals[mid]==(name,digest), f"original bytes not restored for {mid}"

        # All consumed backups should now refuse an extra restore.
        invoke(stage/"RESTORE-CANVAS.cmd",appdata,False)

        evidence={
          "schema":1,"result":"PASS","platform":sys.platform,
          "bundle_sha256":expected,"minecraft":manifest["minecraft"],
          "installed_mod_ids":sorted(mods_expected),
          "checks":["published_checksum","audit_receipt","exact_manifest_install",
                    "repeat_install","restore_previous_release","restore_original_bytes",
                    "double_restore_rejected"]
        }
        pathlib.Path("release-acceptance-windows.json").write_text(
            json.dumps(evidence,indent=2,sort_keys=True)+"\n",encoding="utf-8")
        print(json.dumps(evidence,indent=2))
if __name__=="__main__": main()
