package com.dongsoop.dongsoop.mailverify.controller;

import com.dongsoop.dongsoop.mailverify.dto.MailSendRequest;
import com.dongsoop.dongsoop.mailverify.dto.MailVerifyRequest;
import com.dongsoop.dongsoop.mailverify.passwordupdate.PasswordUpdateMailSender;
import com.dongsoop.dongsoop.mailverify.passwordupdate.PasswordUpdateMailValidator;
import com.dongsoop.dongsoop.mailverify.register.RegisterMailSender;
import com.dongsoop.dongsoop.mailverify.register.RegisterMailValidator;
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

    private final PasswordUpdateMailSender passwordUpdateMailSender;
    private final PasswordUpdateMailValidator passwordUpdateMailValidator;
    private final RegisterMailSender registerMailSender;
    private final RegisterMailValidator registerMailValidator;

    @PostMapping("/send")
    public ResponseEntity<Void> sendRegisterVerifyMail(@RequestBody @Valid MailSendRequest request) {
        registerMailSender.send(request.userEmail());

        return ResponseEntity.noContent()
                .build();
    }

    @PostMapping("/register")
    public ResponseEntity<Void> verifyMailForRegister(@RequestBody @Valid MailVerifyRequest request) {
        registerMailValidator.validateVerificationCodeWithOpportunity(request.userEmail(), request.code());

        return ResponseEntity.noContent()
                .build();
    }

    @PostMapping("/send/password-update")
    public ResponseEntity<Void> sendPasswordChangeMail(@RequestBody @Valid MailSendRequest request) {
        passwordUpdateMailSender.send(request.userEmail());
        return ResponseEntity.noContent()
                .build();
    }

    @PostMapping("/password-update")
    public ResponseEntity<Void> verifyCodeForPasswordUpdate(@RequestBody @Valid MailVerifyRequest request) {
        passwordUpdateMailValidator.validateVerificationCodeWithOpportunity(request.userEmail(), request.code());

        return ResponseEntity.noContent()
                .build();
    }
}
