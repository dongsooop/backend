package com.dongsoop.dongsoop.recruitment.tutoring.controller;

import com.dongsoop.dongsoop.recruitment.tutoring.dto.ApplyTutoringBoardRequest;
import com.dongsoop.dongsoop.recruitment.tutoring.service.TutoringApplyService;
import com.dongsoop.dongsoop.role.entity.RoleType;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
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
    @Secured(value = RoleType.USER_ROLE)
    public ResponseEntity<Void> applyTutoringBoard(@RequestBody ApplyTutoringBoardRequest request) {
        tutoringApplyService.apply(request);

        URI uri = URI.create("/tutoring-board/" + request.boardId());

        return ResponseEntity.created(uri)
                .build();
    }
}
