package com.dongsoop.dongsoop.notification.setting.service.handler;

import com.dongsoop.dongsoop.notification.setting.dto.SettingChanges;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public interface NotificationSettingChangeHandler {

    Class<? extends SettingChanges> getSupportedClass();

    void apply(List<SettingChanges> setting);
}
