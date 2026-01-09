package com.dongsoop.dongsoop.notification.setting.service.handler;

import com.dongsoop.dongsoop.notification.setting.dto.SettingChanges;
import com.dongsoop.dongsoop.notification.setting.dto.SettingDelete;
import com.dongsoop.dongsoop.notification.setting.entity.NotificationSetting;
import com.dongsoop.dongsoop.notification.setting.repository.NotificationSettingRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationSettingChangeDelete implements NotificationSettingChangeHandler {

    private final NotificationSettingRepository notificationSettingRepository;

    @Override
    public Class<? extends SettingChanges> getSupportedClass() {
        return SettingDelete.class;
    }

    @Override
    public void apply(List<SettingChanges> setting) {
        List<NotificationSetting> notificationSettingList = setting.stream()
                .map(SettingChanges::getSetting)
                .toList();

        notificationSettingRepository.deleteAll(notificationSettingList);
    }
}
