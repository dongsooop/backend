package com.dongsoop.dongsoop.notice.preference.service;

import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.department.service.DepartmentService;
import com.dongsoop.dongsoop.memberdevice.entity.MemberDevice;
import com.dongsoop.dongsoop.memberdevice.service.GuestDeviceResolver;
import com.dongsoop.dongsoop.notice.preference.entity.DeviceNoticePreference;
import com.dongsoop.dongsoop.notice.preference.repository.DeviceNoticePreferenceRepository;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GuestNoticePreferenceServiceImpl implements GuestNoticePreferenceService {

    private final GuestDeviceResolver guestDeviceResolver;
    private final DeviceNoticePreferenceRepository preferenceRepository;
    private final DepartmentService departmentService;

    /**
     * 선택한 학과 목록으로 전체 교체한다 (PUT 시맨틱).
     *
     * <p>기존 구독 중 새 목록에 없는 학과는 삭제하고, 새 목록에만 있는 학과는 추가한다.
     * 이미 구독 중인 학과는 건드리지 않는다.
     */
    @Override
    @Transactional
    public void updateDepartments(String deviceToken, Set<DepartmentType> departmentTypes) {
        MemberDevice device = guestDeviceResolver.resolve(deviceToken);

        // 이 디바이스가 현재 구독 중인 학과 목록을 불러온다
        List<DeviceNoticePreference> existing = findPreferences(device);
        Set<DepartmentType> existingTypes = existing.stream()
                .map(preference -> preference.getId().getDepartment().getId())
                .collect(Collectors.toSet());

        // 기존 구독 중 새 요청에 없는 학과 = 제거 대상
        List<DeviceNoticePreference> toRemove = existing.stream()
                .filter(preference -> !departmentTypes.contains(preference.getId().getDepartment().getId()))
                .toList();
        if (!toRemove.isEmpty()) {
            preferenceRepository.deleteAll(toRemove);
        }

        // 새 요청 중 기존에 없던 학과 = 추가 대상 (이미 구독 중인 학과는 건드리지 않는다)
        List<DeviceNoticePreference> toAdd = departmentTypes.stream()
                .filter(departmentType -> !existingTypes.contains(departmentType))
                .map(departmentType -> new DeviceNoticePreference(device, departmentService.getReferenceById(departmentType)))
                .toList();
        if (!toAdd.isEmpty()) {
            preferenceRepository.saveAll(toAdd);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<DepartmentType> getDepartments(String deviceToken) {
        MemberDevice device = guestDeviceResolver.resolve(deviceToken);

        return findPreferences(device).stream()
                .map(preference -> preference.getId().getDepartment().getId())
                .toList();
    }

    private List<DeviceNoticePreference> findPreferences(MemberDevice device) {
        return preferenceRepository.findAllByIdDeviceId(device.getId());
    }
}
