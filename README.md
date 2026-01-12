# Employee Management System — DevSecOps, Logging & Deliverables

This repository contains a microservices-based Employee Management System with API Gateway, Eureka Discovery, Employee/Department services, and a React frontend. This iteration adds:

- DevSecOps tooling:
  - Static analysis with SonarQube (Maven Sonar scanner)
  - Dependency vulnerability scan with OWASP Dependency-Check
  - Docker/Dockerfile scans with Trivy (filesystem and config; optional image scan)
- Logging and traceability:
  - Access and error logs for all APIs
  - User identification and correlation IDs in logs
  - Actuator endpoints for basic service health/metrics
- CI pipeline (GitHub Actions) that builds, tests, and runs the above scans.

## 1) Project structure

- gateway-service: Spring Cloud Gateway (WebFlux), OAuth2 client/resource server, correlation and access logging filters
- employee-service: Spring Boot REST + JPA, OAuth2 resource server, access logging filter
- department-service: Spring Boot REST + JPA, OAuth2 resource server, access logging filter
- discovery-service: Eureka server
- ems_frontend: React client
- docker-compose.yml: Full local stack including Keycloak, Postgres, services
- docker-compose-sonarqube.yml: Local SonarQube stack
- docs/: Architecture and sequence diagrams (Mermaid)
- scripts/: PowerShell helper scripts for Sonar, Dependency-Check, and Trivy

## 2) DevSecOps

### 2.1 SonarQube (static code analysis)

Prerequisites:
- Run local SonarQube: `docker compose -f docker-compose-sonarqube.yml up -d`
- Open SonarQube UI at `http://localhost:9000` and login (default: admin/admin)
- Create a project (manually) or use "Analyze new project"
- Generate a token:
  1. Go to your user avatar (top right) → My Account → Security
  2. Generate new token, copy it (this is your `SONAR_TOKEN`)

Run analysis locally (Windows/PowerShell):

```
./scripts/scan-sonar.ps1 -SonarHostUrl http://localhost:9000 -SonarToken <YOUR_TOKEN>
```

In CI (GitHub Actions):
- Add repository secret `SONAR_TOKEN` with your token value
- Optionally define `SONAR_HOST_URL` as an Actions organization/repo variable if not using default `http://localhost:9000`

The workflow file: `.github/workflows/ci.yml`

### 2.2 OWASP Dependency-Check (dependencies scan)

Local (PowerShell):
```
./scripts/scan-depcheck.ps1 -FailOnCVSS 7
```
Reports are generated under each module’s `target/` directory.

In CI: executed as part of the GitHub Actions pipeline (`mvn org.owasp:dependency-check-maven:check`).

### 2.3 Trivy (Docker and IaC scans)

Install Trivy: https://aquasecurity.github.io/trivy/v0.56/getting-started/installation/

Local (PowerShell):
```
# Filesystem scan (source vulnerabilities)
./scripts/scan-trivy.ps1 -Mode fs -Path .

# Config scan (Dockerfiles, IaC)
./scripts/scan-trivy.ps1 -Mode config -Path .

# Optional: image scan after building images
# docker build -t ems-gateway ./gateway-service
# ./scripts/scan-trivy.ps1 -Mode image -ImageName ems-gateway:latest
```

In CI: filesystem and config scans run automatically, results uploaded to the Security tab (SARIF).

### 2.4 Fixing vulnerabilities

- For code issues: follow SonarQube recommendations (bugs, code smells, security hotspots)
- For dependencies: review `dependency-check` reports and upgrade or suppress via `dependency-check-suppressions.xml` (already present). Keep CVSS threshold at 7 by default.
- For container/IaC issues: address Trivy findings by updating base images, pinning versions, adjusting Dockerfiles and compose manifests.

## 3) Logging and traceability

- All services log access and errors with user and correlation IDs.
  - Correlation header: `X-Correlation-Id` (gateway injects if missing and propagates forward)
  - Username is resolved from Spring Security context (Keycloak) or set to `anonymous`
  - Example pattern (console):
    - gateway-service `application.yml`:
      ``
      logging.pattern.console: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level [%X{correlationId}] [%X{username}] %logger{36} - %msg%n"
      ``
- Access/error logging filters:
  - Gateway: `LoggingWebFilter` and `CorrelationGlobalFilter`
  - Employee: `AccessLoggingFilter`
  - Department: `AccessLoggingFilter`
- Actuator endpoints exposed (health/info/metrics/loggers/prometheus) and liveness/readiness probes enabled.

## 4) How to run locally

- Full stack: `docker compose up -d`
- Open services:
  - Keycloak: http://localhost:8080
  - Eureka: http://localhost:8761
  - Gateway: http://localhost:8888
  - Employee: http://localhost:8041
  - Department: http://localhost:8040
- Actuator endpoints: `http://<service-host>:<port>/actuator` (health, info, metrics, prometheus)

## 5) CI pipeline

The GitHub Actions workflow performs:
- Build + unit tests (JaCoCo)
- OWASP Dependency-Check
- SonarQube scan (if `SONAR_TOKEN` secret is set)
- Trivy filesystem and config scans

File: `.github/workflows/ci.yml`

## 6) Diagrams (deliverables)

- Architecture: `docs/architecture.md` (Mermaid)
- Sequence (example Employee fetch/create): `docs/sequence-employee.md`

You can render Mermaid directly on GitHub or use a Mermaid viewer.

## 7) Screenshots (deliverables)

Create a `docs/screenshots/` folder and add screenshots of:
- SonarQube project dashboard
- Dependency-Check reports summary
- Trivy scan output (FS / Config / Image)

## 8) Notes

- Logging output files are configured to `/app/logs/*.log` when running inside containers; adjust volumes in `docker-compose.yml` if you want to persist logs.
- Ensure Keycloak realm and client configuration matches `application.yml` values (issuer, jwk-set-uri, client-id, secrets).
