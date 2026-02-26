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
import org.springframework.http.ResponseEntity;
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

    @PostMapping
    public ResponseEntity<Void> registerDevice(@RequestBody @Valid DeviceRegisterRequest request) {
        memberDeviceService.registerDevice(request.deviceToken(), request.type());
        fcmService.subscribeTopic(List.of(request.deviceToken()), anonymousTopic);

        return ResponseEntity.noContent().build();
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
     * <p>해당 기기의 JWT를 블랙리스트에 등록하여 이후 모든 요청을 즉시 차단하고,
     * anonymous 토픽을 재구독한 뒤 회원과의 바인딩을 해제한다.
     * 토픽 재구독 실패 시 경고 로그를 남기고 계속 진행하며,
     * 블랙리스트 등록과 바인딩 해제는 항상 실행된다.
     *
     * @param deviceId 강제 로그아웃할 기기의 ID
     * @return 응답 본문 없음 (204 No Content)
     * @throws com.dongsoop.dongsoop.memberdevice.exception.UnregisteredDeviceException     기기가 존재하지 않는 경우
     * @throws com.dongsoop.dongsoop.memberdevice.exception.UnauthorizedDeviceAccessException 본인 소유 기기가 아닌 경우
     */
    @DeleteMapping("/{deviceId}")
    public ResponseEntity<Void> forceLogout(@PathVariable Long deviceId) {
        Long memberId = memberService.getMemberIdByAuthentication();

        String deviceToken = memberDeviceService.getDeviceTokenIfOwned(memberId, deviceId);

        deviceBlacklistService.blacklist(deviceId);

        try {
            fcmService.subscribeTopic(List.of(deviceToken), anonymousTopic);
        } catch (Exception e) {
            log.warn("Failed to resubscribe device {} to anonymous topic: {}", deviceId, e.getMessage());
        }

        memberDeviceService.unbindDevice(deviceId);

        return ResponseEntity.noContent().build();
    }
}
