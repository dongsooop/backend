package com.dongsoop.dongsoop.notification.setting.service.handler;

import com.dongsoop.dongsoop.notification.setting.dto.SettingChanges;
import com.dongsoop.dongsoop.notification.setting.dto.SettingUpdate;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class NotificationSettingChangeUpdate implements NotificationSettingChangeHandler {

    @Override
    public Class<? extends SettingChanges> getSupportedClass() {
        return SettingUpdate.class;
    }

    @Override
    public void apply(List<SettingChanges> changes) {
        changes.stream()
                .filter(change -> Objects.nonNull(change.getSetting())) // null인 경우 제외
                .forEach(s -> s.getSetting().updateEnabled(s.getTargetState())); // null이 아닌 경우 업데이트
    }
}
