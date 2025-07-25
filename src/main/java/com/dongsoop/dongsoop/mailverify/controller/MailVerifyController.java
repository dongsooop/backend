package com.dongsoop.dongsoop.mailverify.controller;

import com.dongsoop.dongsoop.mailverify.dto.MailSendRequest;
import com.dongsoop.dongsoop.mailverify.service.MailVerifyService;
import jakarta.mail.MessagingException;
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

    @PostMapping
    public ResponseEntity<Void> sendVerifyMail(@RequestBody MailSendRequest request) throws MessagingException {
        mailVerifyService.sendMail(request.to());

        return ResponseEntity.ok()
                .build();
    }
}
