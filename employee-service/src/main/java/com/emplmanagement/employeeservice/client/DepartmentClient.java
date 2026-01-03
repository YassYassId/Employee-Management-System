package com.emplmanagement.employeeservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        name = "DEPARTMENT-SERVICE",
        fallback = DepartmentClientFallback.class
)
public interface DepartmentClient {

    @GetMapping("/departments/{id}")
    DepartmentDto getDepartmentById(@PathVariable("id") Long id);

    record DepartmentDto(Long id, String name, String location) {}
}