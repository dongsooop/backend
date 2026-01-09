package com.dongsoop.dongsoop.notification.setting.service.switcher;

import com.dongsoop.dongsoop.member.service.MemberService;
import com.dongsoop.dongsoop.memberdevice.entity.MemberDevice;
import com.dongsoop.dongsoop.memberdevice.exception.UnregisteredDeviceException;
import com.dongsoop.dongsoop.memberdevice.repository.MemberDeviceRepository;
import com.dongsoop.dongsoop.notification.constant.NotificationType;
import com.dongsoop.dongsoop.notification.setting.dto.NotificationSettingUpdate;
import com.dongsoop.dongsoop.notification.setting.dto.SettingChanges;
import com.dongsoop.dongsoop.notification.setting.entity.NotificationSetting;
import com.dongsoop.dongsoop.notification.setting.entity.NotificationSettingId;
import com.dongsoop.dongsoop.notification.setting.repository.NotificationSettingRepository;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class NotificationSettingSwitcher {

    private final MemberService memberService;
    private final MemberDeviceRepository memberDeviceRepository;
    private final NotificationSettingRepository notificationSettingRepository;
    private final SettingChangeClassifier changeClassifier;
    private final NotificationSettingOperator settingOperator;

    public NotificationSettingSwitcher(MemberService memberService,
                                       MemberDeviceRepository memberDeviceRepository,
                                       NotificationSettingRepository notificationSettingRepository,
                                       SettingChangeClassifier changeClassifier,
                                       NotificationSettingOperator settingOperator) {
        this.memberService = memberService;
        this.memberDeviceRepository = memberDeviceRepository;
        this.notificationSettingRepository = notificationSettingRepository;
        this.changeClassifier = changeClassifier;
        this.settingOperator = settingOperator;
    }

    /**
     * 알림 설정 활성화/비활성화 업데이트
     *
     * @param request 알림 설정 요청 정보
     */
    public void updateStatus(NotificationSettingUpdate request) {
        List<MemberDevice> devices = this.resolveDevice(request.deviceToken());
        this.updateEnable(devices, request.notificationTypes(), request.targetState());
    }

    /**
     * 디바이스 알림 설정 업데이트
     *
     * @param devices     디바이스 목록
     * @param types       설정할 알림 타입 목록
     * @param targetState 목표 활성화 상태
     */
    private void updateEnable(List<MemberDevice> devices, Collection<NotificationType> types, boolean targetState) {
        // 이미 저장된 알림 설정 정보 조회
        Map<NotificationSettingId, NotificationSetting> existingSettings = this.loadNotificationSettings(devices,
                types);

        // 변경 사항 타입 계산
        List<SettingChanges> changes = changeClassifier.classify(
                devices, types, existingSettings, targetState
        );

        // 변경 적용
        settingOperator.applyChanges(changes);

        // 로깅
        logChanges(changes);
    }

    // 기존 알림 설정 정보 조회
    private Map<NotificationSettingId, NotificationSetting> loadNotificationSettings(
            Collection<MemberDevice> deviceList,
            Collection<NotificationType> types) {

        List<NotificationSettingId> notificationSettingIdList = deviceList.stream()
                .flatMap(device ->
                        types.stream()
                                .map((notificationType) -> new NotificationSettingId(device, notificationType)))
                .toList();

        // 기존 설정 정보 조회
        List<NotificationSetting> notificationSettingList = this.notificationSettingRepository.findAllById(
                notificationSettingIdList);

        return notificationSettingList.stream()
                .collect(Collectors.toMap(
                        NotificationSetting::getId,
                        ns -> ns
                ));
    }

    // 인증된 회원일 경우 회원의 모든 디바이스, 비회원일 경우 토큰으로 디바이스 조회
    private List<MemberDevice> resolveDevice(String deviceToken) {
        if (memberService.isAuthenticated()) {
            Long memberId = memberService.getMemberIdByAuthentication();
            return memberDeviceRepository.findByMemberId(memberId);
        }

        MemberDevice device = memberDeviceRepository.findByDeviceToken(deviceToken)
                .orElseThrow(UnregisteredDeviceException::new);
        return List.of(device);
    }

    // 변경 사항 로그 기록
    private void logChanges(List<SettingChanges> changes) {
        changes.forEach(settingChange -> log.info(settingChange.getLog()));
    }
}
