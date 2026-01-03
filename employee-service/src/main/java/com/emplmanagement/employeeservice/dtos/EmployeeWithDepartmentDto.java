package com.emplmanagement.employeeservice.dtos;

import com.emplmanagement.employeeservice.client.DepartmentClient;

public record EmployeeWithDepartmentDto(
        Long id,
        String name,
        String position,
        Long departmentId,
        DepartmentClient.DepartmentDto department
) {}