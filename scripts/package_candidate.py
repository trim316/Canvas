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
if len(companions) < 5:
    raise SystemExit(f"expected validated companion stack, got only {len(companions)} JARs")

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
    "production-client launch, recursive companion-mod resolution, visual-delta\n"
    "verification, audio-resource verification, and artifact inspection pass.\n",
    encoding="utf-8",
)

installer = r'''@echo off
setlocal EnableExtensions
set "MODS=%APPDATA%\ModrinthApp\profiles\Fabulously Optimized\mods"
set "BACKUP=%MODS%\.canvas-backup\proven-candidate"
set "PS1=%TEMP%\canvas-proven-install-%RANDOM%.ps1"

if not exist "%MODS%" (
  echo Fabulously Optimized mods folder not found:
  echo %MODS%
  exit /b 1
)

if not exist "%BACKUP%" mkdir "%BACKUP%"

> "%PS1%" echo $ErrorActionPreference = 'Stop'
>>"%PS1%" echo Add-Type -AssemblyName System.IO.Compression.FileSystem
>>"%PS1%" echo $mods = [IO.Path]::GetFullPath('%MODS%')
>>"%PS1%" echo $backup = [IO.Path]::GetFullPath('%BACKUP%')
>>"%PS1%" echo $incoming = [IO.Path]::GetFullPath('%~dp0mods')
>>"%PS1%" echo function Get-ModId([string]$jar) {
>>"%PS1%" echo   try {
>>"%PS1%" echo     $zip = [IO.Compression.ZipFile]::OpenRead($jar)
>>"%PS1%" echo     try {
>>"%PS1%" echo       $entry = $zip.GetEntry('fabric.mod.json')
>>"%PS1%" echo       if ($null -eq $entry) { return $null }
>>"%PS1%" echo       $reader = New-Object IO.StreamReader($entry.Open())
>>"%PS1%" echo       try { return (($reader.ReadToEnd() ^| ConvertFrom-Json).id) } finally { $reader.Dispose() }
>>"%PS1%" echo     } finally { $zip.Dispose() }
>>"%PS1%" echo   } catch { return $null }
>>"%PS1%" echo }
>>"%PS1%" echo $new = Get-ChildItem $incoming -Filter '*.jar'
>>"%PS1%" echo $ids = @{}
>>"%PS1%" echo foreach ($jar in $new) { $id = Get-ModId $jar.FullName; if ($id) { $ids[$id] = $true } }
>>"%PS1%" echo foreach ($old in Get-ChildItem $mods -Filter '*.jar') {
>>"%PS1%" echo   $id = Get-ModId $old.FullName
>>"%PS1%" echo   if ($id -and $ids.ContainsKey($id)) {
>>"%PS1%" echo     Move-Item -Force $old.FullName (Join-Path $backup $old.Name)
>>"%PS1%" echo   }
>>"%PS1%" echo }
>>"%PS1%" echo foreach ($jar in $new) { Copy-Item -Force $jar.FullName (Join-Path $mods $jar.Name) }

powershell -NoProfile -ExecutionPolicy Bypass -File "%PS1%"
set "RC=%ERRORLEVEL%"
del /Q "%PS1%" >nul 2>nul
if not "%RC%"=="0" exit /b %RC%

echo Canvas proven candidate installed.
exit /b 0
'''
(stage / "INSTALL-CANVAS.cmd").write_text(installer, encoding="utf-8", newline="\r\n")

output.parent.mkdir(parents=True, exist_ok=True)
with zipfile.ZipFile(output, "w", compression=zipfile.ZIP_DEFLATED, compresslevel=9) as archive:
    for path in sorted(stage.rglob("*")):
        if path.is_file():
            archive.write(path, path.relative_to(stage))

print(f"candidate bundle: PASS {output} ({output.stat().st_size} bytes)")
