package com.dongsoop.dongsoop.feedback.controller;

import com.dongsoop.dongsoop.feedback.dto.FeedbackCreate;
import com.dongsoop.dongsoop.feedback.service.FeedbackService;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/feedback")
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;

    @PostMapping
    public ResponseEntity<Void> feedback(@Valid @RequestBody FeedbackCreate request) {
        Long id = feedbackService.submitFeedback(request);
        return ResponseEntity.created(URI.create("/feedback/" + id))
                .build();
    }
}
