package com.dongsoop.dongsoop.home.controller;

import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.home.dto.HomeDto;
import com.dongsoop.dongsoop.home.service.HomeService;
import com.dongsoop.dongsoop.member.service.MemberService;
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
     */
    private DepartmentType resolveGuestDepartment(String anonymousKey) {
        if (!StringUtils.hasText(anonymousKey)) {
            return null;
        }

        return guestNoticePreferenceService.getDepartment(anonymousKey)
                .departmentType();
    }
}
