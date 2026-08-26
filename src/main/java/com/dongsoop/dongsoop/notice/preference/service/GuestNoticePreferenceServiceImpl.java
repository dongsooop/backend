package com.dongsoop.dongsoop.notice.preference.service;

import com.dongsoop.dongsoop.department.entity.Department;
import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.department.service.DepartmentService;
import com.dongsoop.dongsoop.memberdevice.entity.MemberDevice;
import com.dongsoop.dongsoop.memberdevice.service.GuestDeviceResolver;
import com.dongsoop.dongsoop.notice.preference.dto.GuestDepartmentResponse;
import com.dongsoop.dongsoop.notice.preference.entity.DeviceNoticePreference;
import com.dongsoop.dongsoop.notice.preference.repository.DeviceNoticePreferenceRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GuestNoticePreferenceServiceImpl implements GuestNoticePreferenceService {

    private final GuestDeviceResolver guestDeviceResolver;
    private final DeviceNoticePreferenceRepository preferenceRepository;
    private final DepartmentService departmentService;

    @Override
    @Transactional
    public void updateDepartment(String anonymousKey, DepartmentType departmentType) {
        MemberDevice device = guestDeviceResolver.resolve(anonymousKey);
        Department department = departmentService.getReferenceById(departmentType);

        Optional<DeviceNoticePreference> existing = preferenceRepository.findByMemberDeviceId(device.getId());
        if (existing.isPresent()) {
            existing.get().updateDepartment(department);
            return;
        }

        preferenceRepository.save(new DeviceNoticePreference(device, department));
    }

    @Override
    @Transactional(readOnly = true)
    public GuestDepartmentResponse getDepartment(String anonymousKey) {
        MemberDevice device = guestDeviceResolver.resolve(anonymousKey);

        DepartmentType departmentType = preferenceRepository.findByMemberDeviceId(device.getId())
                .map(preference -> preference.getDepartment().getId())
                .orElse(null);

        return new GuestDepartmentResponse(departmentType);
    }
}
