package com.dongsoop.dongsoop.memberdevice.controller;

import com.dongsoop.dongsoop.jwt.service.DeviceBlacklistService;
import com.dongsoop.dongsoop.member.service.MemberService;
import com.dongsoop.dongsoop.memberdevice.dto.DeviceRegisterRequest;
import com.dongsoop.dongsoop.memberdevice.dto.MemberDeviceResponse;
import com.dongsoop.dongsoop.memberdevice.service.MemberDeviceService;
import com.dongsoop.dongsoop.notification.service.FCMService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/device")
@RequiredArgsConstructor
@Slf4j
public class MemberDeviceController {

    private final MemberDeviceService memberDeviceService;
    private final MemberService memberService;
    private final FCMService fcmService;
    private final DeviceBlacklistService deviceBlacklistService;

    @Value("${notification.topic.anonymous}")
    private String anonymousTopic;

    /**
     * 디바이스를 등록하거나 기존 디바이스의 토큰을 갱신한다.
     *
     * <p>JWT에 deviceId가 포함된 인증 요청인 경우 기존 디바이스의 토큰을 갱신하고,
     * 미인증 요청이거나 deviceId가 없는 경우 새 디바이스를 등록한다.
     * 새 디바이스 등록 시 anonymous 토픽을 구독한다.
     *
     * @return 디바이스 ID (201 Created)
     */
    @PostMapping
    public ResponseEntity<Void> registerDevice(@RequestBody @Valid DeviceRegisterRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Long existingDeviceId = (auth != null && auth.getDetails() instanceof Long id) ? id : null;
        memberDeviceService.registerDevice(request.deviceToken(), request.type(), existingDeviceId);

        if (existingDeviceId == null) {
            fcmService.subscribeTopic(List.of(request.deviceToken()), anonymousTopic);
        }

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * 현재 인증된 회원의 등록 기기 목록을 조회한다.
     *
     * @param deviceToken 현재 요청을 보낸 기기의 FCM 토큰 ({@code X-Device-Token} 헤더, 선택). 전달 시 해당 기기의 {@code current} 필드가 {@code true}로 반환된다.
     * @return 기기 ID, 타입, 현재 기기 여부 목록 (200 OK)
     */
    @GetMapping("/list")
    public ResponseEntity<List<MemberDeviceResponse>> getDeviceList(
            @RequestHeader(value = "X-Device-Token", required = false) String deviceToken) {
        Long memberId = memberService.getMemberIdByAuthentication();
        List<MemberDeviceResponse> devices = memberDeviceService.getDeviceList(memberId, deviceToken);

        return ResponseEntity.ok(devices);
    }

    /**
     * 특정 기기를 강제 로그아웃 처리한다.
     *
     * <p>WEB 타입 기기는 디바이스 행을 삭제하고, 모바일은 회원 바인딩을 해제한다.
     * 블랙리스트 등록과 anonymous 토픽 재구독(모바일)은 항상 실행된다.
     *
     * @param deviceId 강제 로그아웃할 기기의 ID
     * @return 응답 본문 없음 (204 No Content)
     */
    @DeleteMapping("/{deviceId}")
    public ResponseEntity<Void> forceLogout(@PathVariable Long deviceId) {
        Long memberId = memberService.getMemberIdByAuthentication();

        String deviceToken = memberDeviceService.getDeviceTokenIfOwned(memberId, deviceId);

        deviceBlacklistService.blacklist(deviceId);

        if (deviceToken != null) {
            try {
                fcmService.subscribeTopic(List.of(deviceToken), anonymousTopic);
            } catch (Exception e) {
                log.warn("Failed to resubscribe device {} to anonymous topic: {}", deviceId, e.getMessage());
            }
        }

        memberDeviceService.unbindDevice(deviceId);

        return ResponseEntity.noContent().build();
    }
}
