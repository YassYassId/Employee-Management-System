package com.emplmanagement.employeeservice.controller;


import com.emplmanagement.employeeservice.dtos.CreateEmployeeRequest;
import com.emplmanagement.employeeservice.dtos.EmployeeDto;
import com.emplmanagement.employeeservice.dtos.EmployeeWithDepartmentDto;
import com.emplmanagement.employeeservice.service.EmployeeService;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;


@RestController
@RequestMapping("/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService service;

    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public Page<EmployeeDto> search(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Long departmentId,
            @Parameter(hidden = true) Pageable pageable
    ) {
        return service.findAll(name, departmentId, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public EmployeeWithDepartmentDto getById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EmployeeDto> create(@Valid @RequestBody CreateEmployeeRequest request) {
        EmployeeDto dto = service.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(dto.id()).toUri();
        return ResponseEntity.created(location).body(dto);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public EmployeeDto update(
            @PathVariable Long id,
            @Valid @RequestBody CreateEmployeeRequest request
    ) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}