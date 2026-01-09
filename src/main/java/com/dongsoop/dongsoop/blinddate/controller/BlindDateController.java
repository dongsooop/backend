package com.dongsoop.dongsoop.blinddate.controller;

import com.dongsoop.dongsoop.blinddate.notification.BlindDateNotification;
import com.dongsoop.dongsoop.role.entity.RoleType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/blinddate-notification")
@RequiredArgsConstructor
public class BlindDateController {

    private final BlindDateNotification blindDateNotification;

    @PostMapping
    @Secured(RoleType.ADMIN_ROLE)
    public ResponseEntity<?> sendNotification() {
        blindDateNotification.send();

        return ResponseEntity.noContent()
                .build();
    }
}
