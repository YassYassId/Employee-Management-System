param(
  [string]$SonarHostUrl = "http://localhost:9000",
  [Parameter(Mandatory=$true)][string]$SonarToken
)

# Usage:
#   ./scripts/scan-sonar.ps1 -SonarHostUrl http://localhost:9000 -SonarToken <YOUR_TOKEN>

Write-Host "[INFO] Running SonarQube analysis against $SonarHostUrl" -ForegroundColor Cyan

$env:SONAR_HOST_URL = $SonarHostUrl
$env:SONAR_TOKEN = $SonarToken

& mvnw.cmd -B -q org.sonarsource.scanner.maven:sonar-maven-plugin:sonar "-Dsonar.host.url=$SonarHostUrl" "-Dsonar.token=$SonarToken"

if ($LASTEXITCODE -ne 0) {
  Write-Error "SonarQube analysis failed"
  exit 1
}

Write-Host "[INFO] SonarQube analysis completed" -ForegroundColor Green
