package com.emplmanagement.departmentservice.service;

import com.emplmanagement.departmentservice.dtos.CreateDepartmentRequest;
import com.emplmanagement.departmentservice.dtos.DepartmentDto;
import com.emplmanagement.departmentservice.entity.Department;
import com.emplmanagement.departmentservice.exception.DepartmentNotFoundException;
import com.emplmanagement.departmentservice.repository.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DepartmentService {

    private final DepartmentRepository repository;

    @Transactional(readOnly = true)
    public Page<DepartmentDto> findAll(Pageable pageable) {
        return repository.findAll(pageable)
                .map(this::toDto);
    }

    @Transactional(readOnly = true)
    public DepartmentDto findById(Long id) {
        return toDto(repository.findById(id)
                .orElseThrow(() -> new DepartmentNotFoundException(id)));
    }

    @Transactional
    public DepartmentDto create(CreateDepartmentRequest request) {
        Department department = Department.builder()
                .name(request.getName())
                .location(request.getLocation())
                .build();
        Department saved = repository.save(department);
        return toDto(saved);
    }

    @Transactional
    public DepartmentDto update(Long id, CreateDepartmentRequest request) {
        Department department = repository.findById(id)
                .orElseThrow(() -> new DepartmentNotFoundException(id));
        department.setName(request.getName());
        department.setLocation(request.getLocation());
        return toDto(repository.save(department));
    }

    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new DepartmentNotFoundException(id);
        }
        repository.deleteById(id);
    }

    private DepartmentDto toDto(Department d) {
        return new DepartmentDto(d.getId(), d.getName(), d.getLocation());
    }
}