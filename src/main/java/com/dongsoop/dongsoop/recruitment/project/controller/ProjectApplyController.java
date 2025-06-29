package com.dongsoop.dongsoop.recruitment.project.controller;

import com.dongsoop.dongsoop.recruitment.project.dto.ApplyProjectBoardRequest;
import com.dongsoop.dongsoop.recruitment.project.service.ProjectApplyService;
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
@RequestMapping("/project-apply")
@RequiredArgsConstructor
public class ProjectApplyController {

    private final ProjectApplyService projectApplyService;

    @PostMapping
    @Secured(value = RoleType.USER_ROLE)
    public ResponseEntity<Void> applyProjectBoard(@RequestBody ApplyProjectBoardRequest applyProjectBoardRequest) {
        projectApplyService.apply(applyProjectBoardRequest);

        URI uri = URI.create("/project-board/" + applyProjectBoardRequest.boardId());

        return ResponseEntity.created(uri)
                .build();
    }
}
