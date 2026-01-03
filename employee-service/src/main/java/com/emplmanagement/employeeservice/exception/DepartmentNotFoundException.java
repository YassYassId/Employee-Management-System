package com.emplmanagement.employeeservice.exception;

public class DepartmentNotFoundException extends RuntimeException {
    public DepartmentNotFoundException(Long departmentId) {
        super("Department not found with ID: " + departmentId);
    }
}
