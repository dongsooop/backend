package com.dongsoop.dongsoop.recruitment.study.controller;

import com.dongsoop.dongsoop.recruitment.study.dto.ApplyStudyBoardRequest;
import com.dongsoop.dongsoop.recruitment.study.service.StudyApplyService;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/study-apply")
@RequiredArgsConstructor
public class StudyApplyController {

    private final StudyApplyService studyApplyService;

    @PostMapping
    public ResponseEntity<Void> applyTutoringBoard(@RequestBody ApplyStudyBoardRequest request) {
        studyApplyService.apply(request);

        URI uri = URI.create("/tutoring-board/" + request.boardId());

        return ResponseEntity.created(uri)
                .build();
    }
}
