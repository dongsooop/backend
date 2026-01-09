package com.dongsoop.dongsoop.blinddate.controller;

import com.dongsoop.dongsoop.blinddate.notification.BlindDateNotification;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/blinddate")
@RequiredArgsConstructor
public class BlindDateController {

    private final BlindDateNotification blindDateNotification;

    @PostMapping("/notification")
    public ResponseEntity<?> sendNotification() {
        blindDateNotification.send();

        return ResponseEntity.noContent()
                .build();
    }
}
