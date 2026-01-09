package com.dongsoop.dongsoop.notification.setting.service.handler;

import com.dongsoop.dongsoop.notification.setting.dto.SettingChanges;
import com.dongsoop.dongsoop.notification.setting.dto.SettingSkip;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class NotificationSettingChangeSkip implements NotificationSettingChangeHandler {

    @Override
    public Class<? extends SettingChanges> getSupportedClass() {
        return SettingSkip.class;
    }

    @Override
    public void apply(List<SettingChanges> setting) {
        // Do nothing
    }
}
