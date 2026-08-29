package com.dongsoop.dongsoop.home.controller;

import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.home.dto.HomeDto;
import com.dongsoop.dongsoop.home.service.HomeService;
import com.dongsoop.dongsoop.member.service.MemberService;
import com.dongsoop.dongsoop.memberdevice.exception.UnregisteredDeviceException;
import com.dongsoop.dongsoop.notice.preference.service.GuestNoticePreferenceService;
import com.dongsoop.dongsoop.role.entity.RoleType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/home")
@RequiredArgsConstructor
public class HomeController {

    private final HomeService homeService;
    private final MemberService memberService;
    private final GuestNoticePreferenceService guestNoticePreferenceService;

    @GetMapping("/{departmentType}")
    @Secured(RoleType.USER_ROLE)
    public ResponseEntity<HomeDto> getHomeData(@PathVariable("departmentType") DepartmentType departmentType) {
        Long requesterId = memberService.getMemberIdByAuthentication();
        HomeDto home = homeService.getHome(requesterId, departmentType);

        return ResponseEntity.ok(home);
    }

    @GetMapping
    public ResponseEntity<HomeDto> getHomeDataForAnonymous(
            @RequestHeader(value = "X-Anonymous-Key", required = false) String anonymousKey) {
        DepartmentType departmentType = resolveGuestDepartment(anonymousKey);
        if (departmentType == null) {
            return ResponseEntity.ok(homeService.getHome());
        }

        return ResponseEntity.ok(homeService.getGuestHome(departmentType));
    }

    /**
     * 익명 키가 없거나 학과가 설정되지 않았으면 null 을 반환해 기존 비로그인 홈으로 떨어뜨린다.
     * 홈 화면은 어떤 익명 키 상태에서도 떠야 하므로, 무효한 키(미등록/이미 회원에 연결된 기기)도
     * 실패가 아니라 기본 홈으로 떨어뜨린다.
     *
     * <p>알림은 구독한 모든 학과로 나가지만, 홈 피드는 학과 하나 기준으로 구성되는 기존 구조를
     * 그대로 두고 가장 먼저 구독한 학과 하나만 반영한다. 홈 피드의 다학과 집계는 별도 작업으로 분리한다.
     */
    private DepartmentType resolveGuestDepartment(String anonymousKey) {
        if (!StringUtils.hasText(anonymousKey)) {
            return null;
        }

        try {
            return guestNoticePreferenceService.getDepartments(anonymousKey)
                    .departmentTypes()
                    .stream()
                    .findFirst()
                    .orElse(null);
        } catch (UnregisteredDeviceException exception) {
            return null;
        }
    }
}
