package com.emplmanagement.employeeservice.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateEmployeeRequest(
        @NotBlank String name,
        @NotBlank String position,
        @NotNull Long departmentId
) {}