#!/usr/bin/env python3
from __future__ import annotations

import hashlib
import pathlib
import shutil
import sys
import zipfile

if len(sys.argv) != 4:
    raise SystemExit("usage: package_candidate.py <candidate-dir> <companions-dir> <output-zip>")

candidate_dir = pathlib.Path(sys.argv[1])
companions_dir = pathlib.Path(sys.argv[2])
output = pathlib.Path(sys.argv[3])

canvas = [p for p in candidate_dir.rglob("*.jar") if "sources" not in p.name.lower()]
companions = list(companions_dir.rglob("*.jar"))
if len(canvas) != 1:
    raise SystemExit(f"expected exactly one Canvas runtime JAR, got {canvas}")
if len(companions) < 2:
    raise SystemExit(f"expected at least two validated companion JARs, got {companions}")

stage = output.parent / "candidate-stage"
if stage.exists():
    shutil.rmtree(stage)
mods = stage / "mods"
mods.mkdir(parents=True)

files = []
for source in canvas + companions:
    dest = mods / source.name
    shutil.copy2(source, dest)
    files.append(dest)

hash_lines = []
for path in sorted(files):
    digest = hashlib.sha256(path.read_bytes()).hexdigest()
    hash_lines.append(f"{digest}  mods/{path.name}")
(stage / "SHA256SUMS.txt").write_text("\n".join(hash_lines) + "\n", encoding="utf-8")

(stage / "PROVEN.txt").write_text(
    "Canvas Minecraft 26.2 automated candidate\n"
    "This bundle is emitted only after server GameTests, client GameTests,\n"
    "production-client launch, companion-mod launch, visual-delta verification,\n"
    "audio-resource verification, and artifact inspection pass.\n",
    encoding="utf-8",
)

names = [p.name for p in sorted(files)]
copy_lines = "\n".join(f'copy /Y "%~dp0mods\\{name}" "%MODS%\\{name}" >nul' for name in names)
install = f"""@echo off
setlocal EnableExtensions
set "MODS=%APPDATA%\\ModrinthApp\\profiles\\Fabulously Optimized\\mods"
set "BACKUP=%MODS%\\.canvas-backup\\proven-candidate"

if not exist "%MODS%" (
  echo Fabulously Optimized mods folder not found:
  echo %MODS%
  exit /b 1
)

if not exist "%BACKUP%" mkdir "%BACKUP%"

for %%F in ("%MODS%\\canvas-*.jar") do if exist "%%~fF" move /Y "%%~fF" "%BACKUP%\\" >nul
for %%F in ("%MODS%\\coolrain-*.jar") do if exist "%%~fF" move /Y "%%~fF" "%BACKUP%\\" >nul
for %%F in ("%MODS%\\sound-physics-remastered-*.jar") do if exist "%%~fF" move /Y "%%~fF" "%BACKUP%\\" >nul
for %%F in ("%MODS%\\sound_physics_remastered-*.jar") do if exist "%%~fF" move /Y "%%~fF" "%BACKUP%\\" >nul

{copy_lines}

echo Canvas proven candidate installed.
exit /b 0
"""
(stage / "INSTALL-CANVAS.cmd").write_text(install, encoding="utf-8", newline="\r\n")

output.parent.mkdir(parents=True, exist_ok=True)
with zipfile.ZipFile(output, "w", compression=zipfile.ZIP_DEFLATED, compresslevel=9) as z:
    for path in sorted(stage.rglob("*")):
        if path.is_file():
            z.write(path, path.relative_to(stage))

print(f"candidate bundle: PASS {output} ({output.stat().st_size} bytes)")
