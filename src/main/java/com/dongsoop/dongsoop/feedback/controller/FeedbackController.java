package com.dongsoop.dongsoop.feedback.controller;

import com.dongsoop.dongsoop.feedback.dto.FeedbackCreate;
import com.dongsoop.dongsoop.feedback.dto.FeedbackDetail;
import com.dongsoop.dongsoop.feedback.dto.FeedbackOverview;
import com.dongsoop.dongsoop.feedback.service.FeedbackService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

    @GetMapping("/{id}")
    public ResponseEntity<FeedbackDetail> getFeedbackDetail(@PathVariable Long id) {
        FeedbackDetail detail = feedbackService.getFeedbackDetail(id);
        return ResponseEntity.ok(detail);
    }

    @GetMapping
    public ResponseEntity<FeedbackOverview> getFeedbackOverview() {
        FeedbackOverview detail = feedbackService.getFeedbackOverview();
        return ResponseEntity.ok(detail);
    }

    @GetMapping("/improvement-suggestions")
    public ResponseEntity<List<String>> getImprovementSuggestions(Pageable pageable) {
        List<String> suggestions = feedbackService.getImprovementSuggestions(pageable);
        return ResponseEntity.ok(suggestions);
    }

    @GetMapping("/feature-requests")
    public ResponseEntity<List<String>> getFeatureRequests(Pageable pageable) {
        List<String> suggestions = feedbackService.getFeatureRequests(pageable);
        return ResponseEntity.ok(suggestions);
    }
}
