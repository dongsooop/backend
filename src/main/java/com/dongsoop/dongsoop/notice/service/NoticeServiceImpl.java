package com.dongsoop.dongsoop.notice.service;

import com.dongsoop.dongsoop.department.entity.Department;
import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.department.exception.DepartmentNotFoundException;
import com.dongsoop.dongsoop.department.repository.DepartmentRepository;
import com.dongsoop.dongsoop.memberdevice.exception.UnregisteredDeviceException;
import com.dongsoop.dongsoop.notice.dto.NoticeListResponse;
import com.dongsoop.dongsoop.notice.dto.NoticeRecentIdByDepartment;
import com.dongsoop.dongsoop.notice.preference.service.GuestNoticePreferenceService;
import com.dongsoop.dongsoop.notice.repository.NoticeRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class NoticeServiceImpl implements NoticeService {

    private final NoticeRepository noticeRepository;
    private final DepartmentRepository departmentRepository;
    private final GuestNoticePreferenceService guestNoticePreferenceService;

    public Page<NoticeListResponse> getNoticeByDepartmentType(DepartmentType departmentType, Pageable pageable) {
        Optional<Department> optionalDepartment = departmentRepository.findById(departmentType);
        Department department = optionalDepartment.orElseThrow(() -> new DepartmentNotFoundException(departmentType));

        return noticeRepository.findAllByDepartment(department, pageable);
    }

    /**
     * 비회원이 구독한 학과들의 공지를 통합 조회한다.
     *
     * <p>기기 식별 실패(fid/deviceToken 없음, 미등록, 회원 바인딩된 기기)나 구독 학과가
     * 없는 경우 에러 없이 빈 페이지를 반환한다 — 비회원 화면은 실패 시에도 "설정 없음"과
     * 동일하게 보여야 하는 게 이 표면의 관례이다.
     */
    @Override
    public Page<NoticeListResponse> getNoticeForGuest(String fid, String deviceToken, Pageable pageable) {
        List<DepartmentType> departmentTypes;
        try {
            departmentTypes = guestNoticePreferenceService.getDepartmentTypes(fid, deviceToken);
        } catch (UnregisteredDeviceException e) {
            return Page.empty(pageable);
        }

        if (departmentTypes.isEmpty()) {
            return Page.empty(pageable);
        }

        List<Department> departments = departmentRepository.findAllById(departmentTypes);
        if (departments.isEmpty()) {
            return Page.empty(pageable);
        }

        return noticeRepository.findAllByDepartmentIn(departments, pageable);
    }

    /**
     * 학과별 최신 공지 ID를 가져오는 메서드 NoticeScheduler 클래스의 Scheduled 어노테이션으로 인해 메서드 분리
     *
     * @return 학과별 키를 토대로 값에는 최신 공지 ID를 갖는 Map 반환
     */
    @Transactional(readOnly = true)
    public Map<Department, Long> getNoticeRecentIdMap() {
        List<NoticeRecentIdByDepartment> noticeRecentIdByDepartmentList = noticeRepository.findRecentIdGroupByType();
        if (noticeRecentIdByDepartmentList.isEmpty()) {
            log.warn("No recent notice IDs found. Returning an empty map.");
            return Map.of();
        }

        return transformNoticeRecentIdListToMap(noticeRecentIdByDepartmentList);
    }

    /**
     * 학과별 최신 공지 ID List를 Map으로 변환
     *
     * @param noticeRecentIdByDepartmentList [학과, 최신 공지 ID] 목록
     * @return { 학과: 최신 공지 ID } 구조 반환
     */
    private Map<Department, Long> transformNoticeRecentIdListToMap(
            List<NoticeRecentIdByDepartment> noticeRecentIdByDepartmentList) {
        return noticeRecentIdByDepartmentList.stream()
                .collect(Collectors.toMap(
                        NoticeRecentIdByDepartment::getDepartment,
                        NoticeRecentIdByDepartment::getRecentId
                ));
    }
}
