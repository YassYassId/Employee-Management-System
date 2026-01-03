package com.emplmanagement.employeeservice.service;

import com.emplmanagement.employeeservice.client.DepartmentClient;
import com.emplmanagement.employeeservice.dtos.CreateEmployeeRequest;
import com.emplmanagement.employeeservice.dtos.EmployeeDto;
import com.emplmanagement.employeeservice.dtos.EmployeeWithDepartmentDto;
import com.emplmanagement.employeeservice.entity.Employee;
import com.emplmanagement.employeeservice.exception.DepartmentNotFoundException;
import com.emplmanagement.employeeservice.exception.DepartmentServiceUnavailableException;
import com.emplmanagement.employeeservice.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository repository;
    private final DepartmentClient departmentClient;

    @Transactional(readOnly = true)
    public Page<EmployeeDto> findAll(String name, Long departmentId, Pageable pageable) {
        Page<Employee> page;
        if (name != null && departmentId != null) {
            page = repository.findByNameContainingIgnoreCaseAndDepartmentId(name, departmentId, pageable);
        } else if (name != null) {
            page = repository.findByNameContainingIgnoreCase(name, pageable);
        } else if (departmentId != null) {
            page = repository.findByDepartmentId(departmentId, pageable);
        } else {
            page = repository.findAll(pageable);
        }
        return page.map(this::toDto);
    }

    @Transactional(readOnly = true)
    public EmployeeWithDepartmentDto findById(Long id) {
        Employee emp = repository.findById(id)
                .orElseThrow(() -> new DepartmentNotFoundException(id));
        var dept = departmentClient.getDepartmentById(emp.getDepartmentId());
        return new EmployeeWithDepartmentDto(
                emp.getId(), emp.getName(), emp.getPosition(), emp.getDepartmentId(), dept
        );
    }

    @Transactional
    public EmployeeDto create(CreateEmployeeRequest request) {
        validateDepartment(request.departmentId());
        Employee emp = Employee.builder()
                .name(request.name())
                .position(request.position())
                .departmentId(request.departmentId())
                .build();
        return toDto(repository.save(emp));
    }

    @Transactional
    public EmployeeDto update(Long id, CreateEmployeeRequest request) {
        Employee emp = repository.findById(id)
                .orElseThrow(() -> new DepartmentNotFoundException(id));
        validateDepartment(request.departmentId());
        emp.setName(request.name());
        emp.setPosition(request.position());
        emp.setDepartmentId(request.departmentId());
        return toDto(repository.save(emp));
    }

    @Transactional
    public void delete(Long id) {
        repository.deleteById(id);
    }

    private void validateDepartment(Long departmentId) {
        try {
            departmentClient.getDepartmentById(departmentId);
        } catch (feign.FeignException.NotFound e) {
            throw new DepartmentNotFoundException(departmentId);
        } catch (Exception e) {
            throw new DepartmentServiceUnavailableException(
                    "Failed to validate department existence", e
            );
        }
    }

    private EmployeeDto toDto(Employee e) {
        return new EmployeeDto(e.getId(), e.getName(), e.getPosition(), e.getDepartmentId());
    }
}