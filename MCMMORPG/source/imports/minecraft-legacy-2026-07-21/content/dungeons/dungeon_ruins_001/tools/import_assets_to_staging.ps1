param(
  [switch]$Apply,
  [switch]$UseFinalIntake,
  [switch]$AllowReviewCandidates,
  [switch]$AllowLegacyScaffoldApply,
  [string]$ProjectRoot = (Resolve-Path "$PSScriptRoot\..\..\..\..").Path,
  [string]$StagingRoot = $null
)

$ErrorActionPreference = "Stop"

$packRoot = Resolve-Path "$PSScriptRoot\.."
if (-not $StagingRoot) {
  $StagingRoot = "Server $([char]0x2014) kopia\1.21.11"
}
$stageRoot = Join-Path $ProjectRoot $StagingRoot
$intakeRoot = Join-Path $packRoot "intake"
$backupRoot = Join-Path $stageRoot "codex-backups\dungeon_ruins_001\$(Get-Date -Format yyyyMMddHHmmss)"

if (-not (Test-Path $stageRoot)) {
  throw "Staging root not found: $stageRoot"
}

if ($Apply -and -not $UseFinalIntake -and -not $AllowLegacyScaffoldApply) {
  throw "Blocked unsafe legacy scaffold apply. Legacy scaffold is not the DR001 runtime contract. Use -UseFinalIntake for final DR001 intake. Use -AllowLegacyScaffoldApply only for intentional legacy scaffold testing with explicit approval."
}

if ($AllowReviewCandidates -and -not $UseFinalIntake) {
  throw "-AllowReviewCandidates is only valid with -UseFinalIntake."
}

function Get-HashShort {
  param([string]$Path)
  if (-not (Test-Path -LiteralPath $Path)) {
    return "missing"
  }
  return (Get-FileHash -LiteralPath $Path -Algorithm SHA256).Hash.ToLowerInvariant().Substring(0, 12)
}

function Invoke-FinalAssetValidation {
  param([switch]$AllowReviewCandidates)

  $validator = Join-Path $packRoot "tools\validate_final_assets.mjs"
  if (-not (Test-Path -LiteralPath $validator)) {
    throw "Final asset validator not found: $validator"
  }

  $node = Get-Command node -ErrorAction SilentlyContinue
  if (-not $node) {
    throw "Node is required to validate final intake assets."
  }

  if ($AllowReviewCandidates) {
    Write-Host "Running final asset validator in review-candidate mode..."
    & $node.Source $validator "--allow-review-candidates"
  } else {
    Write-Host "Running final asset validator..."
    & $node.Source $validator
  }
  if ($LASTEXITCODE -ne 0) {
    throw "Final intake validation failed. Import aborted before staging writes."
  }
}

function New-Operation {
  param(
    [string]$Source,
    [string]$Target,
    [string]$Label
  )
  return @{
    Source = $Source
    Target = $Target
    Label = $Label
  }
}

if ($UseFinalIntake) {
  Invoke-FinalAssetValidation -AllowReviewCandidates:$AllowReviewCandidates
  $operations = @(
    (New-Operation `
      -Source (Join-Path $intakeRoot "nexo") `
      -Target (Join-Path $stageRoot "plugins\Nexo\pack\assets\nexo") `
      -Label "Nexo final resource-pack assets"),
    (New-Operation `
      -Source (Join-Path $intakeRoot "modelengine") `
      -Target (Join-Path $stageRoot "plugins\ModelEngine\blueprints") `
      -Label "ModelEngine final blueprints"),
    (New-Operation `
      -Source (Join-Path $intakeRoot "schematics") `
      -Target (Join-Path $stageRoot "plugins\Iris\packs\dungeon_ruins_001\schematics") `
      -Label "Iris final schematics"),
    (New-Operation `
      -Source (Join-Path $intakeRoot "sounds\dr001") `
      -Target (Join-Path $stageRoot "plugins\Nexo\pack\assets\nexo\sounds\dr001") `
      -Label "Nexo final custom sounds"),
    (New-Operation `
      -Source (Join-Path $intakeRoot "nexo_runtime") `
      -Target (Join-Path $stageRoot "plugins\Nexo") `
      -Label "Nexo final runtime sound registry")
  )
} else {
  $operations = @(
    (New-Operation `
      -Source (Join-Path $packRoot "resourcepack\assets\nexo") `
      -Target (Join-Path $stageRoot "plugins\Nexo\pack\assets\nexo") `
      -Label "Nexo legacy scaffold resource-pack assets (not DR001 final)"),
    (New-Operation `
      -Source (Join-Path $packRoot "modelengine\blueprints") `
      -Target (Join-Path $stageRoot "plugins\ModelEngine\blueprints") `
      -Label "ModelEngine legacy scaffold blueprints (not DR001 final)"),
    (New-Operation `
      -Source (Join-Path $packRoot "iris\pack\images") `
      -Target (Join-Path $stageRoot "plugins\Iris\packs\dungeon_ruins_001\images") `
      -Label "Iris legacy scaffold layout image (not DR001 final)"),
    (New-Operation `
      -Source (Join-Path $packRoot "iris\schematics") `
      -Target (Join-Path $stageRoot "plugins\Iris\packs\dungeon_ruins_001\schematics") `
      -Label "Iris legacy scaffold schematic source assets (not DR001 final)")
  )
}

Write-Host "Dungeon Ruins 001 staging asset import"
Write-Host "Mode: $(if ($Apply) { 'APPLY' } else { 'DRY-RUN' })"
Write-Host "Asset source: $(if ($UseFinalIntake) { 'FINAL INTAKE' } else { 'LEGACY SCAFFOLD REFERENCE' })"
if ($UseFinalIntake -and $AllowReviewCandidates) {
  Write-Host "Review mode: enabled for staging-only runtime smoke; final approval status is unchanged."
}
if (-not $UseFinalIntake) {
  Write-Host "Warning: legacy scaffold dry-run is reference-only for DR001 release prep."
  if ($Apply -and $AllowLegacyScaffoldApply) {
    Write-Host "Warning: -AllowLegacyScaffoldApply is enabled. This is intentional legacy scaffold testing, not DR001 runtime evidence."
  }
}
Write-Host "Pack: $packRoot"
Write-Host "Staging: $stageRoot"

$totalCopied = 0
$totalSkipped = 0
$totalBlocked = 0

foreach ($op in $operations) {
  if (-not (Test-Path $op.Source)) {
    $totalBlocked++
    throw "Missing source for $($op.Label): $($op.Source)"
  }

  $files = Get-ChildItem -Path $op.Source -Recurse -File |
    Where-Object { $_.Name -ne ".gitkeep" }

  Write-Host ""
  Write-Host "$($op.Label): $($files.Count) files"
  Write-Host "  Source: $($op.Source)"
  Write-Host "  Target: $($op.Target)"

  foreach ($file in $files) {
    $relative = $file.FullName.Substring($op.Source.Length).TrimStart('\', '/')
    $target = Join-Path $op.Target $relative
    $hash = Get-HashShort -Path $file.FullName
    $exists = Test-Path -LiteralPath $target
    $same = $false
    if ($exists) {
      $same = (Get-HashShort -Path $target) -eq $hash
    }

    if ($same) {
      $totalSkipped++
      Write-Host "  skipped  $relative sha256:$hash"
      continue
    }

    if ($Apply) {
      $targetDir = Split-Path -Parent $target
      if (-not (Test-Path $targetDir)) {
        New-Item -ItemType Directory -Path $targetDir -Force | Out-Null
      }
      if ($exists) {
        $backupRelative = $target.Substring($stageRoot.Length).TrimStart('\', '/')
        $backup = Join-Path $backupRoot $backupRelative
        $backupDir = Split-Path -Parent $backup
        if (-not (Test-Path $backupDir)) {
          New-Item -ItemType Directory -Path $backupDir -Force | Out-Null
        }
        Copy-Item -LiteralPath $target -Destination $backup -Force
      }
      Copy-Item -LiteralPath $file.FullName -Destination $target -Force
      $totalCopied++
      Write-Host "  copied   $relative sha256:$hash"
    } else {
      Write-Host "  would-copy $relative sha256:$hash"
    }
  }
}

Write-Host ""
Write-Host "Summary: copied=$totalCopied skipped=$totalSkipped blocked=$totalBlocked"
if ($Apply) {
  Write-Host "Import complete. Review plugin logs after the approved staging reload/generation commands."
} else {
  if ($UseFinalIntake -and $AllowReviewCandidates) {
    Write-Host "Dry-run complete. Re-run with -Apply only for the approved staging-only runtime smoke."
  } else {
    Write-Host "Dry-run complete. Re-run with -Apply only after review approval."
  }
}
