package com.emplmanagement.departmentservice.dtos;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Value;

@Value
public class CreateDepartmentRequest {
    @NotBlank String name;
    @NotBlank String location;
}