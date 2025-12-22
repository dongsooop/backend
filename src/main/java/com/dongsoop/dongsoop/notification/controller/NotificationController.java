package com.dongsoop.dongsoop.notification.controller;

import com.dongsoop.dongsoop.member.service.MemberService;
import com.dongsoop.dongsoop.notification.dto.EventNotification;
import com.dongsoop.dongsoop.notification.dto.NotificationOverview;
import com.dongsoop.dongsoop.notification.dto.NotificationReadRequest;
import com.dongsoop.dongsoop.notification.service.NotificationService;
import com.dongsoop.dongsoop.role.entity.RoleType;
import jakarta.validation.Valid;
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
    private final MemberService memberService;

    @GetMapping
    @Secured(RoleType.USER_ROLE)
    public ResponseEntity<NotificationOverview> getNotifications(Pageable pageable) {
        Long requesterId = memberService.getMemberIdByAuthentication();

        NotificationOverview notifications = notificationService.getNotifications(pageable, requesterId);

        return ResponseEntity.ok(notifications);
    }

    @PostMapping
    @Secured(RoleType.ADMIN_ROLE)
    public ResponseEntity<Void> publishEventNotification(@Valid @RequestBody EventNotification request) {
        notificationService.sendEventNotification(request);
        return ResponseEntity.noContent()
                .build();
    }

    @PostMapping("/read")
    @Secured(RoleType.USER_ROLE)
    public ResponseEntity<Void> readNotification(@RequestBody NotificationReadRequest request) {
        Long requesterId = memberService.getMemberIdByAuthentication();

        notificationService.read(request.id(), requesterId);

        return ResponseEntity.noContent()
                .build();
    }

    @PostMapping("/read-all")
    @Secured(RoleType.USER_ROLE)
    public ResponseEntity<Void> readAllNotification() {
        Long requesterId = memberService.getMemberIdByAuthentication();

        notificationService.readAll(requesterId);

        return ResponseEntity.noContent()
                .build();
    }

    @DeleteMapping("/{id}")
    @Secured(RoleType.USER_ROLE)
    public ResponseEntity<Void> deleteNotifications(@PathVariable("id") Long id) {
        Long requesterId = memberService.getMemberIdByAuthentication();

        notificationService.deleteMemberNotification(id, requesterId);

        return ResponseEntity.noContent()
                .build();
    }
}
