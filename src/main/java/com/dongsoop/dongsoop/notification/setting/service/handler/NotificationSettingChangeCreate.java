package com.dongsoop.dongsoop.notification.setting.service.handler;

import com.dongsoop.dongsoop.notification.setting.dto.SettingChanges;
import com.dongsoop.dongsoop.notification.setting.dto.SettingCreate;
import com.dongsoop.dongsoop.notification.setting.entity.NotificationSetting;
import com.dongsoop.dongsoop.notification.setting.repository.NotificationSettingRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationSettingChangeCreate implements NotificationSettingChangeHandler {

    private final NotificationSettingRepository notificationSettingRepository;

    @Override
    public Class<? extends SettingChanges> getSupportedClass() {
        return SettingCreate.class;
    }

    @Override
    public void apply(List<SettingChanges> changes) {
        List<NotificationSetting> notificationSettingList = changes.stream()
                .map(SettingChanges::getSetting)
                .toList();

        if (notificationSettingList.isEmpty()) {
            return;
        }
        
        notificationSettingRepository.saveAll(notificationSettingList);
    }
}
