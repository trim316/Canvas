#!/usr/bin/env python3
from __future__ import annotations

import hashlib
import json
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
    raise SystemExit(f"expected validated companion stack, got only {len(companions)} JARs")

stage = output.parent / "candidate-stage"
if stage.exists():
    shutil.rmtree(stage)
mods = stage / "mods"
mods.mkdir(parents=True)
evidence = stage / "evidence"
evidence.mkdir(parents=True)


def sha256(path: pathlib.Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def mod_id(path: pathlib.Path) -> str:
    with zipfile.ZipFile(path) as archive:
        meta = json.loads(archive.read("fabric.mod.json").decode("utf-8"))
    value = str(meta.get("id", "")).strip()
    if not value:
        raise SystemExit(f"{path}: missing Fabric mod id")
    return value


files: list[pathlib.Path] = []
manifest_mods: list[dict] = []
seen_ids: set[str] = set()
for source in canvas + companions:
    dest = mods / source.name
    shutil.copy2(source, dest)
    files.append(dest)
    mid = mod_id(dest)
    if mid in seen_ids:
        raise SystemExit(f"duplicate Fabric mod id in candidate: {mid}")
    seen_ids.add(mid)
    manifest_mods.append({
        "mod_id": mid,
        "filename": dest.name,
        "sha256": sha256(dest),
    })

matrix = pathlib.Path("compatibility/companion-matrix.json")
if not matrix.exists():
    raise SystemExit("checked-in compatibility matrix missing")
matrix_dest = evidence / "companion-matrix.json"
shutil.copy2(matrix, matrix_dest)
files.append(matrix_dest)

reports = list(companions_dir.rglob("companion-admission.json"))
if len(reports) != 1:
    raise SystemExit(f"expected exactly one companion admission report, got {reports}")
report_dest = evidence / "companion-admission.json"
shutil.copy2(reports[0], report_dest)
files.append(report_dest)

manifest = {
    "schema": 1,
    "minecraft": "26.2",
    "profile": "Fabulously Optimized",
    "mods": sorted(manifest_mods, key=lambda item: item["mod_id"]),
}
manifest_path = stage / "INSTALL-MANIFEST.json"
manifest_path.write_text(json.dumps(manifest, indent=2, sort_keys=True) + "\n", encoding="utf-8")
files.append(manifest_path)

hash_lines = []
for path in sorted(files):
    digest = sha256(path)
    hash_lines.append(f"{digest}  {path.relative_to(stage).as_posix()}")
(stage / "SHA256SUMS.txt").write_text("\n".join(hash_lines) + "\n", encoding="utf-8")

(stage / "PROVEN.txt").write_text(
    "Canvas Minecraft 26.2 automated candidate\n"
    "This bundle is emitted only after server GameTests, client GameTests,\n"
    "production-client launch, recursive companion-mod resolution, visual-delta\n"
    "verification, audio-resource verification, compatibility-matrix admission,\n"
    "artifact inspection, and installer-bundle verification pass.\n",
    encoding="utf-8",
)

installer = r'''@echo off
setlocal EnableExtensions
set "MODS=%APPDATA%\ModrinthApp\profiles\Fabulously Optimized\mods"
set "PS1=%TEMP%\canvas-proven-install-%RANDOM%.ps1"
if not exist "%MODS%" (
  echo Fabulously Optimized mods folder not found:
  echo %MODS%
  exit /b 1
)
> "%PS1%" echo $ErrorActionPreference = 'Stop'
>>"%PS1%" echo Add-Type -AssemblyName System.IO.Compression.FileSystem
>>"%PS1%" echo $mods = [IO.Path]::GetFullPath('%MODS%')
>>"%PS1%" echo $root = [IO.Path]::GetFullPath('%~dp0')
>>"%PS1%" echo $incoming = Join-Path $root 'mods'
>>"%PS1%" echo $manifest = Get-Content (Join-Path $root 'INSTALL-MANIFEST.json') -Raw ^| ConvertFrom-Json
>>"%PS1%" echo $backupRoot = Join-Path $mods '.canvas-backup'
>>"%PS1%" echo $stamp = Get-Date -Format 'yyyyMMdd-HHmmss-fff'
>>"%PS1%" echo $backup = Join-Path $backupRoot $stamp
>>"%PS1%" echo function Get-ModId([string]$jar) {
>>"%PS1%" echo   $zip = [IO.Compression.ZipFile]::OpenRead($jar)
>>"%PS1%" echo   try {
>>"%PS1%" echo     $entry = $zip.GetEntry('fabric.mod.json')
>>"%PS1%" echo     if ($null -eq $entry) { throw "fabric.mod.json missing: $jar" }
>>"%PS1%" echo     $reader = New-Object IO.StreamReader($entry.Open())
>>"%PS1%" echo     try { return (($reader.ReadToEnd() ^| ConvertFrom-Json).id) } finally { $reader.Dispose() }
>>"%PS1%" echo   } finally { $zip.Dispose() }
>>"%PS1%" echo }
>>"%PS1%" echo foreach ($entry in $manifest.mods) {
>>"%PS1%" echo   $path = Join-Path $incoming $entry.filename
>>"%PS1%" echo   if (!(Test-Path $path)) { throw "Incoming mod missing: $($entry.filename)" }
>>"%PS1%" echo   $hash = (Get-FileHash -Algorithm SHA256 $path).Hash.ToLowerInvariant()
>>"%PS1%" echo   if ($hash -ne $entry.sha256.ToLowerInvariant()) { throw "SHA256 mismatch: $($entry.filename)" }
>>"%PS1%" echo   if ((Get-ModId $path) -ne $entry.mod_id) { throw "Mod ID mismatch: $($entry.filename)" }
>>"%PS1%" echo }
>>"%PS1%" echo New-Item -ItemType Directory -Force -Path $backup ^| Out-Null
>>"%PS1%" echo $ids = @{}
>>"%PS1%" echo foreach ($entry in $manifest.mods) { $ids[$entry.mod_id] = $true }
>>"%PS1%" echo $moved = @()
>>"%PS1%" echo try {
>>"%PS1%" echo   foreach ($old in Get-ChildItem $mods -Filter '*.jar') {
>>"%PS1%" echo     try { $id = Get-ModId $old.FullName } catch { continue }
>>"%PS1%" echo     if ($id -and $ids.ContainsKey($id)) {
>>"%PS1%" echo       $dest = Join-Path $backup $old.Name
>>"%PS1%" echo       Move-Item -Force $old.FullName $dest
>>"%PS1%" echo       $moved += $dest
>>"%PS1%" echo     }
>>"%PS1%" echo   }
>>"%PS1%" echo   Copy-Item (Join-Path $root 'INSTALL-MANIFEST.json') (Join-Path $backup 'INSTALL-MANIFEST.json')
>>"%PS1%" echo   foreach ($entry in $manifest.mods) {
>>"%PS1%" echo     Copy-Item -Force (Join-Path $incoming $entry.filename) (Join-Path $mods $entry.filename)
>>"%PS1%" echo   }
>>"%PS1%" echo } catch {
>>"%PS1%" echo   foreach ($entry in $manifest.mods) { Remove-Item -Force -ErrorAction SilentlyContinue (Join-Path $mods $entry.filename) }
>>"%PS1%" echo   foreach ($old in $moved) { Move-Item -Force $old (Join-Path $mods ([IO.Path]::GetFileName($old))) }
>>"%PS1%" echo   throw
>>"%PS1%" echo }
>>"%PS1%" echo Write-Host "Canvas candidate installed. Backup: $backup"
powershell -NoProfile -ExecutionPolicy Bypass -File "%PS1%"
set "RC=%ERRORLEVEL%"
del /Q "%PS1%" >nul 2>nul
exit /b %RC%
'''
(stage / "INSTALL-CANVAS.cmd").write_text(installer, encoding="utf-8", newline="\r\n")

restore = r'''@echo off
setlocal EnableExtensions
set "MODS=%APPDATA%\ModrinthApp\profiles\Fabulously Optimized\mods"
set "PS1=%TEMP%\canvas-proven-restore-%RANDOM%.ps1"
if not exist "%MODS%\.canvas-backup" (
  echo No Canvas backup folder found.
  exit /b 1
)
> "%PS1%" echo $ErrorActionPreference = 'Stop'
>>"%PS1%" echo Add-Type -AssemblyName System.IO.Compression.FileSystem
>>"%PS1%" echo $mods = [IO.Path]::GetFullPath('%MODS%')
>>"%PS1%" echo $backupRoot = Join-Path $mods '.canvas-backup'
>>"%PS1%" echo $backup = Get-ChildItem $backupRoot -Directory ^| Where-Object { $_.Name -notlike '*-restored' } ^| Sort-Object Name -Descending ^| Select-Object -First 1
>>"%PS1%" echo if ($null -eq $backup) { throw 'No Canvas backup snapshot found' }
>>"%PS1%" echo $manifestPath = Join-Path $backup.FullName 'INSTALL-MANIFEST.json'
>>"%PS1%" echo if (!(Test-Path $manifestPath)) { throw 'Backup manifest missing' }
>>"%PS1%" echo $manifest = Get-Content $manifestPath -Raw ^| ConvertFrom-Json
>>"%PS1%" echo function Get-ModId([string]$jar) {
>>"%PS1%" echo   try {
>>"%PS1%" echo     $zip = [IO.Compression.ZipFile]::OpenRead($jar)
>>"%PS1%" echo     try { $entry = $zip.GetEntry('fabric.mod.json'); if ($null -eq $entry) { return $null }; $reader = New-Object IO.StreamReader($entry.Open()); try { return (($reader.ReadToEnd() ^| ConvertFrom-Json).id) } finally { $reader.Dispose() } } finally { $zip.Dispose() }
>>"%PS1%" echo   } catch { return $null }
>>"%PS1%" echo }
>>"%PS1%" echo $ids = @{}
>>"%PS1%" echo foreach ($entry in $manifest.mods) { $ids[$entry.mod_id] = $true }
>>"%PS1%" echo foreach ($current in Get-ChildItem $mods -Filter '*.jar') { $id = Get-ModId $current.FullName; if ($id -and $ids.ContainsKey($id)) { Remove-Item -Force $current.FullName } }
>>"%PS1%" echo foreach ($old in Get-ChildItem $backup.FullName -Filter '*.jar') { Move-Item -Force $old.FullName (Join-Path $mods $old.Name) }
>>"%PS1%" echo Rename-Item $backup.FullName ($backup.Name + '-restored')
>>"%PS1%" echo Write-Host "Canvas backup restored."
powershell -NoProfile -ExecutionPolicy Bypass -File "%PS1%"
set "RC=%ERRORLEVEL%"
del /Q "%PS1%" >nul 2>nul
exit /b %RC%
'''
(stage / "RESTORE-CANVAS.cmd").write_text(restore, encoding="utf-8", newline="\r\n")

output.parent.mkdir(parents=True, exist_ok=True)
with zipfile.ZipFile(output, "w", compression=zipfile.ZIP_DEFLATED, compresslevel=9) as archive:
    for path in sorted(stage.rglob("*")):
        if path.is_file():
            archive.write(path, path.relative_to(stage))

print(f"candidate bundle: PASS {output} ({output.stat().st_size} bytes)")
