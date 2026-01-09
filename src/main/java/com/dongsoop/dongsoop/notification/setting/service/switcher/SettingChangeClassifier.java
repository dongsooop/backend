package com.dongsoop.dongsoop.notification.setting.service.switcher;

import com.dongsoop.dongsoop.memberdevice.entity.MemberDevice;
import com.dongsoop.dongsoop.notification.constant.NotificationType;
import com.dongsoop.dongsoop.notification.setting.dto.SettingChanges;
import com.dongsoop.dongsoop.notification.setting.dto.SettingCreate;
import com.dongsoop.dongsoop.notification.setting.dto.SettingDelete;
import com.dongsoop.dongsoop.notification.setting.dto.SettingSkip;
import com.dongsoop.dongsoop.notification.setting.dto.SettingUpdate;
import com.dongsoop.dongsoop.notification.setting.entity.NotificationSetting;
import com.dongsoop.dongsoop.notification.setting.entity.NotificationSettingId;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class SettingChangeClassifier {

    // 주어진 디바이스와 알림 타입에 대해 설정 변경 분류
    public List<SettingChanges> classification(
            Collection<MemberDevice> devices,
            Collection<NotificationType> types,
            Map<NotificationSettingId, NotificationSetting> existingSettings,
            boolean targetState
    ) {

        return devices.stream()
                .flatMap((device) -> types.stream().map((type) -> {
                    // 기존 설정 조회, 없으면 기본값 생성
                    NotificationSettingId id = new NotificationSettingId(device, type);
                    NotificationSetting existing = existingSettings.getOrDefault(id, null);

                    return determineChange(device, type, existing, targetState);
                }))
                .toList();
    }

    private SettingChanges determineChange(
            MemberDevice device,
            NotificationType type,
            NotificationSetting existing,
            boolean targetState
    ) {
        // 기본 정책과 일치하면 삭제
        if (type.getDefaultActiveState() == targetState) {
            // 기존 설정이 없으면 스킵
            if (existing == null) {
                return new SettingSkip(null, device, type, targetState);
            }

            return new SettingDelete(existing, device, type, targetState);
        }

        // 기존 설정이 없으면 생성
        if (existing == null) {
            return new SettingCreate(null, device, type, targetState);
        }

        // 상태가 다르면 업데이트, 같으면 스킵
        if (!existing.isSameState(targetState)) {
            return new SettingUpdate(existing, device, type, targetState);
        }

        return new SettingSkip(existing, device, type, targetState);
    }
}