#!/usr/bin/env pwsh
param(
  [Alias('n')][switch]$DryRun,
  [Alias('y')][switch]$Yes,
  [Alias('h','?')][switch]$Help,
  [string]$Directory = '.'
)

if ($Help) {
  Write-Output "Usage: `clean.ps1` [-n] [-y] [directory]"
  Write-Output "  -n    dry-run (list files without deleting)"
  Write-Output "  -y    skip confirmation"
  exit 0
}

try {
  $files = Get-ChildItem -Path $Directory -Recurse -Filter '*.class' -File -ErrorAction Stop
} catch {
  Write-Error "Path not found: $Directory"
  exit 2
}

if (-not $files) {
  Write-Output "No .class files found in $Directory."
  exit 0
}

Write-Output "Found $($files.Count) .class files in $Directory."
if ($DryRun) {
  $files | ForEach-Object { $_.FullName }
  exit 0
}

if (-not $Yes) {
  $ans = Read-Host "Delete these files? [y/N]"
  if ($ans -notin @('y','Y')) {
    Write-Output "Aborted."
    exit 1
  }
}

$deleted = 0
foreach ($f in $files) {
  try {
    Remove-Item -LiteralPath $f.FullName -Force -ErrorAction Stop
    Write-Output "Deleted $($f.FullName)"
    $deleted++
  } catch {
    Write-Warning "Failed to delete $($f.FullName): $_"
  }
}

Write-Output "Deleted $deleted files."
