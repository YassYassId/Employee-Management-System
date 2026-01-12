# Trivy scans for source (fs), config (Dockerfiles), and optional images
# Usage examples:
#   ./scripts/scan-trivy.ps1 -Mode fs
#   ./scripts/scan-trivy.ps1 -Mode config
#   ./scripts/scan-trivy.ps1 -Mode image -ImageName ems-gateway:latest

param(
  [ValidateSet('fs','config','image')][string]$Mode = 'fs',
  [string]$Path = '.',
  [string]$ImageName = ''
)

function Ensure-TrivyInstalled {
  $trivy = Get-Command trivy -ErrorAction SilentlyContinue
  if (-not $trivy) {
    Write-Error "Trivy is not installed. Install from https://aquasecurity.github.io/trivy/v0.56/getting-started/installation/"
    exit 1
  }
}

Ensure-TrivyInstalled

switch ($Mode) {
  'fs' {
    Write-Host "[INFO] Running Trivy filesystem scan on $Path" -ForegroundColor Cyan
    trivy fs --severity CRITICAL,HIGH --format table $Path
  }
  'config' {
    Write-Host "[INFO] Running Trivy config scan (Dockerfiles/manifests) on $Path" -ForegroundColor Cyan
    trivy config --severity CRITICAL,HIGH --format table $Path
  }
  'image' {
    if ([string]::IsNullOrWhiteSpace($ImageName)) {
      Write-Error "-ImageName is required when Mode=image"
      exit 1
    }
    Write-Host "[INFO] Running Trivy image scan on $ImageName" -ForegroundColor Cyan
    trivy image --severity CRITICAL,HIGH --format table $ImageName
  }
}

if ($LASTEXITCODE -ne 0) {
  Write-Error "Trivy scan finished with findings or errors. Review the output above."
  exit 1
}

Write-Host "[INFO] Trivy scan completed" -ForegroundColor Green
