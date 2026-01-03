package com.emplmanagement.employeeservice.dtos;

public record EmployeeDto(
        Long id,
        String name,
        String position,
        Long departmentId
) {}