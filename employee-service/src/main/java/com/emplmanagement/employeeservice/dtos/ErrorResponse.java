package com.emplmanagement.employeeservice.dtos;

import java.time.Instant;

public record ErrorResponse(
        Instant timestamp,
        String error,
        String message
) {
    public ErrorResponse(String error, String message) {
        this(Instant.now(), error, message);
    }
}