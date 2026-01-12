# Runs OWASP Dependency-Check against the Maven project
# Usage: ./scripts/scan-depcheck.ps1 [-FailOnCVSS 7]

param(
  [int]$FailOnCVSS = 7
)

Write-Host "[INFO] Running OWASP Dependency-Check (CVSS>=$FailOnCVSS)" -ForegroundColor Cyan

$cmd = "org.owasp:dependency-check-maven:check"
& mvnw.cmd -B -q $cmd "-Dformat=ALL" "-DfailBuildOnCVSS=$FailOnCVSS" "-DsuppressionFiles=dependency-check-suppressions.xml"

if ($LASTEXITCODE -ne 0) {
  Write-Error "Dependency-Check found vulnerabilities above threshold (CVSS>=$FailOnCVSS)"
  exit 1
}

Write-Host "[INFO] Dependency-Check completed. See reports under each module's target/dependency-check-report.*" -ForegroundColor Green
