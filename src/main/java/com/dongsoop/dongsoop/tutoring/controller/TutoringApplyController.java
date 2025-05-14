package com.dongsoop.dongsoop.tutoring.controller;

import com.dongsoop.dongsoop.tutoring.dto.ApplyTutoringBoardRequest;
import com.dongsoop.dongsoop.tutoring.service.TutoringApplyService;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tutoring-apply")
@RequiredArgsConstructor
public class TutoringApplyController {

    private final TutoringApplyService tutoringApplyService;

    @PostMapping
    public ResponseEntity<Void> applyTutoringBoard(@RequestBody ApplyTutoringBoardRequest applyTutoringBoardRequest) {
        Long tutoringBoardId = applyTutoringBoardRequest.getTutoringBoardId();
        tutoringApplyService.apply(tutoringBoardId);

        URI uri = URI.create("/tutoring-board/" + tutoringBoardId);

        return ResponseEntity.created(uri)
                .build();
    }
}
