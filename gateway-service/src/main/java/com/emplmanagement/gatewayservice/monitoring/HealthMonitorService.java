package com.emplmanagement.gatewayservice.monitoring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@Service
public class HealthMonitorService implements HealthIndicator {

    private static final Logger logger = LoggerFactory.getLogger(HealthMonitorService.class);
    private static final Logger HEALTH_LOG = LoggerFactory.getLogger("HEALTH_LOG");

    private final Map<String, ServiceStatus> serviceStatuses = new ConcurrentHashMap<>();
    private LocalDateTime startTime;
    private long requestCount = 0;
    private long errorCount = 0;

    public HealthMonitorService() {
        this.startTime = LocalDateTime.now();
        logger.info("Health Monitor initialized at {}", startTime);
    }

    /**
     * Log service health status every 5 minutes
     */
    @Scheduled(fixedRate = 300000) // 5 minutes
    public void logHealthStatus() {
        Map<String, Object> healthInfo = new HashMap<>();

        healthInfo.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
        healthInfo.put("uptime", getUptime());
        healthInfo.put("totalRequests", requestCount);
        healthInfo.put("totalErrors", errorCount);
        healthInfo.put("errorRate", calculateErrorRate());

        HEALTH_LOG.info("Service Health Status: {}", healthInfo);

        // Log memory usage
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024;
        long maxMemory = runtime.maxMemory() / 1024 / 1024;

        logger.info("Memory Usage: {} MB / {} MB ({}%)",
                usedMemory, maxMemory, (usedMemory * 100) / maxMemory);

        // Warn if memory usage is high
        if ((usedMemory * 100) / maxMemory > 80) {
            logger.warn("HIGH MEMORY USAGE DETECTED: {}%", (usedMemory * 100) / maxMemory);
        }
    }

    /**
     * Log detailed health status every hour
     */
    @Scheduled(fixedRate = 3600000) // 1 hour
    public void logDetailedHealthStatus() {
        logger.info("=== Detailed Health Report ===");
        logger.info("Service uptime: {}", getUptime());
        logger.info("Total requests processed: {}", requestCount);
        logger.info("Total errors: {}", errorCount);
        logger.info("Error rate: {}%", String.format("%.2f", calculateErrorRate()));

        // Log thread info
        ThreadGroup threadGroup = Thread.currentThread().getThreadGroup();
        logger.info("Active threads: {}", threadGroup.activeCount());

        // Log service dependencies
        serviceStatuses.forEach((service, status) -> {
            logger.info("Dependency {}: {} (last check: {})",
                    service, status.isHealthy() ? "UP" : "DOWN", status.getLastCheck());
        });

        logger.info("=== End Health Report ===");
    }

    @Override
    public Health health() {
        Map<String, Object> details = new HashMap<>();

        details.put("uptime", getUptime());
        details.put("requestCount", requestCount);
        details.put("errorCount", errorCount);
        details.put("errorRate", calculateErrorRate());

        // Check service dependencies
        boolean allHealthy = serviceStatuses.values().stream()
                .allMatch(ServiceStatus::isHealthy);

        if (!allHealthy) {
            details.put("unhealthyServices",
                    serviceStatuses.entrySet().stream()
                            .filter(e -> !e.getValue().isHealthy())
                            .map(Map.Entry::getKey)
                            .toList());

            return Health.down().withDetails(details).build();
        }

        return Health.up().withDetails(details).build();
    }

    public void incrementRequestCount() {
        requestCount++;
    }

    public void incrementErrorCount() {
        errorCount++;
        logger.warn("Error count increased to: {}", errorCount);
    }

    public void updateServiceStatus(String serviceName, boolean healthy) {
        ServiceStatus status = serviceStatuses.computeIfAbsent(
                serviceName,
                k -> new ServiceStatus()
        );

        boolean wasHealthy = status.isHealthy();
        status.setHealthy(healthy);
        status.setLastCheck(LocalDateTime.now());

        // Log status changes
        if (wasHealthy && !healthy) {
            logger.error("SERVICE DOWN ALERT: {} is now unhealthy", serviceName);
            HEALTH_LOG.error("Service {} changed status: UP -> DOWN", serviceName);
        } else if (!wasHealthy && healthy) {
            logger.info("SERVICE RECOVERED: {} is now healthy", serviceName);
            HEALTH_LOG.info("Service {} changed status: DOWN -> UP", serviceName);
        }
    }

    private String getUptime() {
        long seconds = java.time.Duration.between(startTime, LocalDateTime.now()).getSeconds();
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;

        return String.format("%dh %dm %ds", hours, minutes, secs);
    }

    private double calculateErrorRate() {
        if (requestCount == 0) {
            return 0.0;
        }
        return (errorCount * 100.0) / requestCount;
    }

    private static class ServiceStatus {
        private boolean healthy = true;
        private LocalDateTime lastCheck = LocalDateTime.now();

        public boolean isHealthy() {
            return healthy;
        }

        public void setHealthy(boolean healthy) {
            this.healthy = healthy;
        }

        public LocalDateTime getLastCheck() {
            return lastCheck;
        }

        public void setLastCheck(LocalDateTime lastCheck) {
            this.lastCheck = lastCheck;
        }
    }
}