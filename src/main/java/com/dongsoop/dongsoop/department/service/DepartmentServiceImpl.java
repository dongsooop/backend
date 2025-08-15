package com.dongsoop.dongsoop.department.service;

import com.dongsoop.dongsoop.department.entity.Department;
import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.department.exception.DepartmentNotFoundException;
import com.dongsoop.dongsoop.department.repository.DepartmentRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional(readOnly = true)
    public List<Department> getAllDepartments() {
        return departmentRepository.findAll();
    }
}
