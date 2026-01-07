package com.dongsoop.dongsoop.notification.setting.controller;

import com.dongsoop.dongsoop.notification.constant.NotificationType;
import com.dongsoop.dongsoop.notification.setting.dto.NotificationSettingFindRequest;
import com.dongsoop.dongsoop.notification.setting.dto.NotificationSettingRequest;
import com.dongsoop.dongsoop.notification.setting.service.NotificationSettingService;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/notification-settings")
public class NotificationSettingController {

    private final NotificationSettingService notificationSettingService;

    @PostMapping("/find")
    public ResponseEntity<Map<NotificationType, Boolean>> getNotificationSettings(
            @Valid @RequestBody NotificationSettingFindRequest request) {
        Map<NotificationType, Boolean> notificationSettings = notificationSettingService.getNotificationSettings(
                request);

        return ResponseEntity.ok(notificationSettings);
    }

    @PostMapping("/enable")
    public ResponseEntity<Void> enableNotification(@Valid @RequestBody NotificationSettingRequest request) {
        notificationSettingService.enableNotification(request);

        return ResponseEntity.noContent()
                .build();
    }

    @PostMapping("/disable")
    public ResponseEntity<Void> disableNotification(@Valid @RequestBody NotificationSettingRequest request) {
        notificationSettingService.disableNotification(request);

        return ResponseEntity.noContent()
                .build();
    }
}
