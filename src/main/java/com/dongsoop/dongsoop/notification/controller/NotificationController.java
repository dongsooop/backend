package com.dongsoop.dongsoop.notification.controller;

import com.dongsoop.dongsoop.notification.dto.NotificationOverview;
import com.dongsoop.dongsoop.notification.dto.NotificationReadRequest;
import com.dongsoop.dongsoop.notification.service.NotificationService;
import com.dongsoop.dongsoop.role.entity.RoleType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @Secured(RoleType.USER_ROLE)
    public ResponseEntity<NotificationOverview> getNotifications(Pageable pageable) {
        NotificationOverview notifications = notificationService.getNotifications(pageable);

        return ResponseEntity.ok(notifications);
    }

    @PostMapping("/read")
    @Secured(RoleType.USER_ROLE)
    public ResponseEntity<Void> readNotification(@RequestBody NotificationReadRequest request) {
        notificationService.read(request.id());

        return ResponseEntity.noContent()
                .build();
    }

    @DeleteMapping("/{id}")
    @Secured(RoleType.USER_ROLE)
    public ResponseEntity<Void> deleteNotifications(@PathVariable("id") Long id) {
        notificationService.deleteMemberNotification(id);

        return ResponseEntity.noContent()
                .build();
    }
}
