package com.emplmanagement.departmentservice.dtos;

import jakarta.validation.constraints.NotBlank;
import lombok.Value;

@Value
public class DepartmentDto {
    Long id;
    @NotBlank String name;
    @NotBlank String location;
}