package com.dongsoop.dongsoop.department.service;

import com.dongsoop.dongsoop.department.entity.Department;
import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.department.repository.DepartmentRepository;
import com.dongsoop.dongsoop.exception.domain.department.DepartmentNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentRepository departmentRepository;

    @Override
    public Department getReferenceById(DepartmentType departmentType) {
        validateDepartment(departmentType);
        
        return departmentRepository.getReferenceById(departmentType);
    }

    private void validateDepartment(DepartmentType departmentType) {
        boolean isExists = departmentRepository.existsById(departmentType);
        if (!isExists) {
            throw new DepartmentNotFoundException(departmentType);
        }
    }
}
