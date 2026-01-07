package com.dongsoop.dongsoop.notification.setting.service.switcher;

import com.dongsoop.dongsoop.member.service.MemberService;
import com.dongsoop.dongsoop.memberdevice.entity.MemberDevice;
import com.dongsoop.dongsoop.memberdevice.exception.UnregisteredDeviceException;
import com.dongsoop.dongsoop.memberdevice.repository.MemberDeviceRepository;
import com.dongsoop.dongsoop.notification.constant.NotificationType;
import com.dongsoop.dongsoop.notification.setting.dto.NotificationSettingRequest;
import com.dongsoop.dongsoop.notification.setting.entity.NotificationSetting;
import com.dongsoop.dongsoop.notification.setting.entity.NotificationSettingId;
import com.dongsoop.dongsoop.notification.setting.repository.NotificationSettingRepository;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class NotificationSettingSwitcher {

    private final MemberService memberService;
    private final MemberDeviceRepository memberDeviceRepository;
    private final NotificationSettingRepository notificationSettingRepository;

    public NotificationSettingSwitcher(MemberService memberService,
                                       MemberDeviceRepository memberDeviceRepository,
                                       NotificationSettingRepository notificationSettingRepository) {
        this.memberService = memberService;
        this.memberDeviceRepository = memberDeviceRepository;
        this.notificationSettingRepository = notificationSettingRepository;
    }

    protected abstract boolean shouldEnable();

    /**
     * 알림 설정 활성화/비활성화 업데이트
     *
     * @param request 알림 설정 요청 정보
     */
    public void updateStatus(NotificationSettingRequest request) {
        // 회원 디바이스 토큰 전체 설정 변경
        if (memberService.isAuthenticated()) {
            Long memberId = memberService.getMemberIdByAuthentication();
            List<MemberDevice> devices = memberDeviceRepository.findByMemberId(memberId);

            this.updateEnable(devices, request.notificationType());

            return;
        }

        // 비회원 디바이스 토큰 단일 설정 변경
        MemberDevice device = memberDeviceRepository.findByDeviceToken(request.deviceToken())
                .orElseThrow(UnregisteredDeviceException::new);
        this.updateEnable(List.of(device), request.notificationType());
    }

    /**
     * 디바이스 알림 설정 업데이트
     *
     * @param deviceList       디바이스 목록
     * @param notificationType 설정할 알림 타입
     */
    private void updateEnable(List<MemberDevice> deviceList, NotificationType notificationType) {
        List<Optional<NotificationSetting>> optionalNotificationSettingList = deviceList.stream()
                .map(device -> new NotificationSettingId(device, notificationType))
                .map(this.notificationSettingRepository::findById)
                .toList();

        // 알림 타입의 기본 정책과 동일한 상태로 바꾸려고 할 때
        if (notificationType.getDefaultActiveState() == shouldEnable()) {
            // DB에서 설정 정보 제거
            this.deleteSetting(optionalNotificationSettingList, deviceList, notificationType);
            return;
        }

        // 알림 타입의 기본 정책과 동일하지 않을 때
        for (int deviceIndex = 0; deviceIndex < deviceList.size(); deviceIndex++) {
            MemberDevice device = deviceList.get(deviceIndex);
            Optional<NotificationSetting> optionalNotificationSetting = optionalNotificationSettingList.get(
                    deviceIndex);

            // 저장된 데이터가 없는 경우 새로 저장
            if (optionalNotificationSetting.isEmpty()) {
                this.saveSettings(device, notificationType, shouldEnable());

                return;
            }

            // 설정하려는 상태와 저장된 값이 동일한 경우 종료
            NotificationSetting notificationSetting = optionalNotificationSetting.get();
            if (notificationSetting.isSameState(shouldEnable())) {
                this.loggingNoSetting(device.getDeviceToken(), notificationType, shouldEnable());
                return;
            }

            // 저장된 설정 업데이트
            this.updateSettings(notificationSetting, device.getDeviceToken(), notificationType);
        }
    }

    // 기본 정책과 일치하여 설정을 제거
    private void deleteSetting(List<Optional<NotificationSetting>> optionalNotificationSettingList,
                               List<MemberDevice> deviceList,
                               NotificationType notificationType) {
        // DB에 존재하는 경우 설정 정보 제거
        optionalNotificationSettingList.forEach(
                optionalNotificationSetting -> optionalNotificationSetting.ifPresent(
                        notificationSettingRepository::delete));

        // 로깅용 디바이스 토큰 문자열 생성
        String deviceTokensString = deviceList.stream()
                .map(MemberDevice::getDeviceToken)
                .collect(Collectors.joining(",", "", ","));

        this.loggingSettingMatchedDefaultPolicy("[" + deviceTokensString + "]", notificationType, shouldEnable());
    }

    // 기본 정책과 일치하여 설정이 제거된 경우 로깅
    private void loggingSettingMatchedDefaultPolicy(String deviceToken, NotificationType notificationType,
                                                    boolean isEnabled) {
        log.info(
                "Notification setting matches default policy. Removed custom setting from DB. deviceToken: {}, notificationType: {}, isEnabled: {}",
                deviceToken, notificationType, isEnabled);
    }

    // 알림 상태 새로 저장
    private void saveSettings(MemberDevice memberDevice, NotificationType notificationType, boolean isEnabled) {
        log.info("Notification setting not found. deviceToken: {}, notificationType: {}, isEnabled: {}",
                memberDevice.getDeviceToken(), notificationType, isEnabled);
        this.notificationSettingRepository.save(
                new NotificationSetting(memberDevice, notificationType, isEnabled));
    }

    // 변경사항이 없는 경우 로깅
    private void loggingNoSetting(String deviceToken, NotificationType notificationType, boolean isEnabled) {
        log.info(
                "Notification setting already in desired state. No update needed. deviceToken: {}, notificationType: {}, isEnabled: {}",
                deviceToken, notificationType, isEnabled);
    }

    // 설정 상태 업데이트
    private void updateSettings(NotificationSetting notificationSetting, String deviceToken,
                                NotificationType notificationType) {
        notificationSetting.updateEnabled(shouldEnable());
        log.info("Notification setting updated successfully. deviceToken: {}, notificationType: {}, isEnabled: {}",
                deviceToken, notificationType, shouldEnable());
    }
}
