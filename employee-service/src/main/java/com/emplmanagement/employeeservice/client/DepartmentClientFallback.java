package com.emplmanagement.employeeservice.client;

import com.emplmanagement.employeeservice.exception.DepartmentServiceUnavailableException;
import org.springframework.stereotype.Component;

@Component
public class DepartmentClientFallback implements DepartmentClient {

    @Override
    public DepartmentDto getDepartmentById(Long id) {
        throw new DepartmentServiceUnavailableException(
                "Department service is currently unavailable. Please try again later."
        );
    }
}