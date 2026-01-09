package com.dongsoop.dongsoop.notification.setting.service.handler;

import com.dongsoop.dongsoop.notification.setting.dto.SettingChanges;
import com.dongsoop.dongsoop.notification.setting.dto.SettingUpdate;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class NotificationSettingChangeUpdate implements NotificationSettingChangeHandler {

    @Override
    public Class<? extends SettingChanges> getSupportedClass() {
        return SettingUpdate.class;
    }

    @Override
    public void apply(List<SettingChanges> setting) {
        setting.forEach(s -> s.getSetting().updateEnabled(s.getTargetState()));
    }
}
