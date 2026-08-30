package com.dongsoop.dongsoop.home.controller;

import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.home.dto.HomeDto;
import com.dongsoop.dongsoop.home.service.HomeService;
import com.dongsoop.dongsoop.member.service.MemberService;
import com.dongsoop.dongsoop.memberdevice.exception.UnregisteredDeviceException;
import com.dongsoop.dongsoop.notice.preference.service.NoticePreferenceService;
import com.dongsoop.dongsoop.role.entity.RoleType;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
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
    private final NoticePreferenceService noticePreferenceService;

    @GetMapping("/{departmentType}")
    @Secured(RoleType.USER_ROLE)
    public ResponseEntity<HomeDto> getHomeData(@PathVariable("departmentType") DepartmentType departmentType) {
        Long requesterId = memberService.getMemberIdByAuthentication();
        HomeDto home = homeService.getHome(requesterId, departmentType);

        return ResponseEntity.ok(home);
    }

    @GetMapping
    public ResponseEntity<HomeDto> getHomeDataForAnonymous(
            @RequestHeader(value = "X-Device-Fid", required = false) String fid,
            @RequestHeader(value = "X-Device-Token", required = false) String deviceToken) {
        HomeDto home = getHomeForDevice(fid, deviceToken);

        return ResponseEntity.ok(home);
    }

    /**
     * fid/deviceToken이 없거나 미등록 기기여도 에러 없이 기본(대학 공지만) 홈으로 떨어뜨린다 —
     * 홈 화면은 어떤 상태에서도 항상 떠야 한다. {@code getHome(Set)}이 빈 Set도 안전하게
     * 처리하므로(대학 공지 자동 포함) 별도의 무인자 폴백 메서드가 필요 없다.
     */
    private HomeDto getHomeForDevice(String fid, String deviceToken) {
        try {
            List<DepartmentType> departmentTypes = noticePreferenceService.getDepartmentTypes(fid, deviceToken);
            return homeService.getHome(Set.copyOf(departmentTypes));
        } catch (UnregisteredDeviceException e) {
            return homeService.getHome(Set.of());
        }
    }
}
