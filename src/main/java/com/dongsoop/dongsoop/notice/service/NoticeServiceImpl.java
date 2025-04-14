package com.dongsoop.dongsoop.notice.service;

import com.dongsoop.dongsoop.department.entity.Department;
import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.department.repository.DepartmentRepository;
import com.dongsoop.dongsoop.exception.domain.department.DepartmentNotFoundException;
import com.dongsoop.dongsoop.notice.dto.NoticeListResponse;
import com.dongsoop.dongsoop.notice.repository.NoticeRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NoticeServiceImpl implements NoticeService {

    private final NoticeRepository noticeRepository;
    private final DepartmentRepository departmentRepository;

    public Page<NoticeListResponse> getNoticeByDepartmentType(DepartmentType departmentType, Pageable pageable) {
        Optional<Department> optionalDepartment = departmentRepository.findById(departmentType);
        Department department = optionalDepartment.orElseThrow(DepartmentNotFoundException::new);

        return noticeRepository.findAllByDepartment(department, pageable);
    }
}
