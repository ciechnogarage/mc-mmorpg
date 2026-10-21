param(
  [switch]$Apply,
  [switch]$ApproveConfigOnlyStagingSync,
  [string]$ProjectRoot = (Resolve-Path "$PSScriptRoot\..\..\..\..").Path,
  [string]$ManifestPath = (Join-Path $PSScriptRoot "..\staging\DR001_CONFIG_ONLY_SYNC_MANIFEST.json")
)

$ErrorActionPreference = "Stop"

if ($Apply -and -not $ApproveConfigOnlyStagingSync) {
  throw "Config-only staging sync apply is blocked. Re-run with -ApproveConfigOnlyStagingSync only after explicit human/operator approval."
}

if (-not (Test-Path -LiteralPath $ManifestPath)) {
  throw "Config-only sync manifest not found: $ManifestPath"
}

$manifest = Get-Content -LiteralPath $ManifestPath -Raw | ConvertFrom-Json
$targetRootRelative = $manifest.target_root -replace '/', '\'
$stageRoot = Join-Path $ProjectRoot $targetRootRelative
$pluginsRoot = Join-Path $stageRoot "plugins"

if ($manifest.target_environment -ne "staging") {
  throw "Refusing config-only sync: manifest target_environment is not staging."
}
if ($stageRoot -notlike "*Server * kopia*\1.21.11") {
  throw "Refusing config-only sync: target root does not look like the approved staging copy: $stageRoot"
}
if ($stageRoot -match '(?i)\\Server\\|production|live') {
  throw "Refusing config-only sync: target root looks like live/production: $stageRoot"
}
if (-not (Test-Path -LiteralPath $pluginsRoot)) {
  throw "Staging plugins root not found: $pluginsRoot"
}

$binaryExtensions = @(".bbmodel", ".png", ".ogg", ".schem", ".schematic", ".zip")
$recognizedTargets = @(
  '^plugins[\\/]MythicMobs[\\/](mobs|skills|droptables)[\\/][A-Za-z0-9_]+\.ya?ml$',
  '^plugins[\\/]MythicDungeons[\\/]maps[\\/]dungeon_ruins_001[\\/](config|functions)\.yml$'
)

function Convert-ToRepoPath {
  param([string]$PathText)
  return ($PathText -replace '/', '\')
}

function Test-RecognizedTarget {
  param([string]$RelativePath)
  foreach ($pattern in $recognizedTargets) {
    if ($RelativePath -match $pattern) {
      return $true
    }
  }
  return $false
}

Write-Host "DR001 config-only staging sync"
Write-Host "Mode: $(if ($Apply) { 'APPLY' } else { 'DRY-RUN' })"
Write-Host "Active namespace: $($manifest.active_namespace.upper) / $($manifest.active_namespace.lower)"
Write-Host "Target environment: $($manifest.target_environment)"
Write-Host "Target root: $stageRoot"
Write-Host "Excluded assets: ModelEngine .bbmodel, Nexo resource-pack files, Iris schematics/images, sounds, zip packs, legacy scaffold assets"
Write-Host "Final asset gate: not bypassed; no asset import is performed by this script."

$planned = 0
$copied = 0

foreach ($op in $manifest.operations) {
  if (-not $op.safe_for_config_only_sync) {
    throw "Operation is not marked safe for config-only sync: $($op.source)"
  }

  $sourceRelative = Convert-ToRepoPath $op.source
  $targetRelative = Convert-ToRepoPath $op.target
  $sourcePath = Join-Path $ProjectRoot $sourceRelative
  $targetPath = Join-Path $ProjectRoot $targetRelative

  if (-not (Test-Path -LiteralPath $sourcePath)) {
    throw "Missing source: $sourceRelative"
  }
  if ($sourceRelative -match 'dungeon_ruins_001_[A-Za-z0-9_]+') {
    throw "Refusing legacy scaffold source path: $sourceRelative"
  }
  if ($targetRelative -match 'dungeon_ruins_001_[A-Za-z0-9_]+') {
    throw "Refusing legacy scaffold target path: $targetRelative"
  }

  $sourceExt = [System.IO.Path]::GetExtension($sourcePath).ToLowerInvariant()
  $targetExt = [System.IO.Path]::GetExtension($targetPath).ToLowerInvariant()
  if ($binaryExtensions -contains $sourceExt -or $binaryExtensions -contains $targetExt) {
    throw "Refusing binary asset operation: $sourceRelative -> $targetRelative"
  }

  $targetUnderPlugins = $targetRelative.Substring($targetRootRelative.Length).TrimStart('\', '/')
  if ($targetUnderPlugins -notlike "plugins\*") {
    throw "Target is not under staging plugins root: $targetRelative"
  }
  if (-not (Test-RecognizedTarget $targetUnderPlugins)) {
    throw "Target is not a recognized DR001 plugin-native config path: $targetUnderPlugins"
  }

  $planned++
  $exists = Test-Path -LiteralPath $targetPath
  Write-Host ""
  Write-Host "$($op.plugin_owner) / $($op.config_type)"
  Write-Host "  source: $sourceRelative"
  Write-Host "  target: $targetRelative"
  Write-Host "  target exists: $exists"
  Write-Host "  notes: $($op.notes)"

  if ($Apply) {
    $targetDir = Split-Path -Parent $targetPath
    if (-not (Test-Path -LiteralPath $targetDir)) {
      New-Item -ItemType Directory -Path $targetDir -Force | Out-Null
    }
    Copy-Item -LiteralPath $sourcePath -Destination $targetPath -Force
    $copied++
    Write-Host "  copied"
  } else {
    Write-Host "  would-copy"
  }
}

Write-Host ""
Write-Host "Summary: planned=$planned copied=$copied"
if (-not $Apply) {
  Write-Host "Dry-run complete. Apply was not run."
}
