package com.emplmanagement.employeeservice.exception;

public class DepartmentServiceUnavailableException extends RuntimeException {
    public DepartmentServiceUnavailableException(String message) {
        super(message);
    }

    public DepartmentServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}