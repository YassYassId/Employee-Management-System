package com.emplmanagement.departmentservice.exception;

public class DepartmentNotFoundException extends RuntimeException {
    public DepartmentNotFoundException(Long id) {
        super("Department not found with ID: " + id);
    }
}