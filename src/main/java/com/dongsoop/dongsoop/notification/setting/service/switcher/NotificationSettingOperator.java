package com.dongsoop.dongsoop.notification.setting.service.switcher;

import com.dongsoop.dongsoop.notification.setting.dto.SettingChanges;
import com.dongsoop.dongsoop.notification.setting.exception.NotificationSettingHandlerDuplicatedException;
import com.dongsoop.dongsoop.notification.setting.exception.NotificationSettingHandlerNotFoundException;
import com.dongsoop.dongsoop.notification.setting.service.handler.NotificationSettingChangeHandler;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class NotificationSettingOperator {

    private final Map<Class<? extends SettingChanges>, NotificationSettingChangeHandler> handlerMap;

    public NotificationSettingOperator(List<NotificationSettingChangeHandler> handlers) {
        this.handlerMap = handlers.stream()
                .collect(Collectors.toMap(
                        NotificationSettingChangeHandler::getSupportedClass,
                        handler -> handler,
                        (existing, duplicate) -> {
                            throw new NotificationSettingHandlerDuplicatedException(existing, duplicate);
                        }));
    }

    @Transactional
    public void applyChanges(List<SettingChanges> changes) {
        // 1. 변경 사항을 클래스 타입별로 미리 그룹화 (O(N))
        Map<Class<? extends SettingChanges>, List<SettingChanges>> changesMap =
                changes.stream().collect(Collectors.groupingBy(SettingChanges::getClass));

        changesMap.forEach((key, value) -> {
            NotificationSettingChangeHandler handler = this.handlerMap.getOrDefault(key, null);
            if (handler == null) {
                throw new NotificationSettingHandlerNotFoundException(key);
            }

            handler.apply(value);
        });
    }
}
