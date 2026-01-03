package com.emplmanagement.employeeservice.repository;

import com.emplmanagement.employeeservice.entity.Employee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    Page<Employee> findByNameContainingIgnoreCase(String name, Pageable pageable);
    Page<Employee> findByDepartmentId(Long departmentId, Pageable pageable);
    Page<Employee> findByNameContainingIgnoreCaseAndDepartmentId(String name, Long departmentId, Pageable pageable);
}
