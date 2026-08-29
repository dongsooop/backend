package com.dongsoop.dongsoop.home.controller;

import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.home.dto.HomeDto;
import com.dongsoop.dongsoop.home.service.HomeService;
import com.dongsoop.dongsoop.member.service.MemberService;
import com.dongsoop.dongsoop.memberdevice.exception.UnregisteredDeviceException;
import com.dongsoop.dongsoop.notice.preference.service.GuestNoticePreferenceService;
import com.dongsoop.dongsoop.role.entity.RoleType;
import java.util.Set;
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
            @RequestHeader(value = "X-Device-Token", required = false) String deviceToken) {
        HomeDto home = getHomeForGuest(deviceToken);

        return ResponseEntity.ok(home);
    }

    private HomeDto getHomeForGuest(String deviceToken) {
        if (!StringUtils.hasText(deviceToken)) {
            return homeService.getHome();
        }

        try {
            Set<DepartmentType> departmentTypes = Set.copyOf(
                    guestNoticePreferenceService.getDepartments(deviceToken).departmentTypes());
            if (departmentTypes.isEmpty()) {
                return homeService.getHome();
            }

            return homeService.getHome(departmentTypes);
        } catch (UnregisteredDeviceException e) {
            return homeService.getHome();
        }
    }
}
