package com.dongsoop.dongsoop.mailverify.controller;

import com.dongsoop.dongsoop.mailverify.dto.MailSendRequest;
import com.dongsoop.dongsoop.mailverify.dto.MailVerifyRequest;
import com.dongsoop.dongsoop.mailverify.service.MailVerifyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/mail-verify")
@RequiredArgsConstructor
public class MailVerifyController {

    private final MailVerifyService mailVerifyService;

    @PostMapping("/send")
    public ResponseEntity<Void> sendVerifyMail(@RequestBody @Valid MailSendRequest request) {
        mailVerifyService.sendMail(request.userEmail());

        return ResponseEntity.noContent()
                .build();
    }

    @PostMapping("/send/password-change")
    public ResponseEntity<Void> sendPasswordChangeMail(@RequestBody @Valid MailSendRequest request) {
        mailVerifyService.sendPasswordChangeMail(request.userEmail());
        return ResponseEntity.noContent()
                .build();
    }

    @PostMapping
    public ResponseEntity<Void> verifyMail(@RequestBody @Valid MailVerifyRequest request) {
        mailVerifyService.validateVerificationCode(request.userEmail(), request.code());

        return ResponseEntity.noContent()
                .build();
    }
}
