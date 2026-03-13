package com.dongsoop.dongsoop.appcheck.controller;

import com.dongsoop.dongsoop.appcheck.web.WebAppCheckService;
import com.dongsoop.dongsoop.appcheck.web.dto.WebAppCheckResponse;
import com.dongsoop.dongsoop.recaptcha.annotation.ReCaptchaRequired;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/web-app-check")
public class WebAppCheckController {

    private final WebAppCheckService webAppCheckService;

    @PostMapping
    @ReCaptchaRequired(action = "web_app_check")
    public ResponseEntity<WebAppCheckResponse> issue() {
        String token = webAppCheckService.issue();
        return ResponseEntity.ok(new WebAppCheckResponse(token));
    }
}
