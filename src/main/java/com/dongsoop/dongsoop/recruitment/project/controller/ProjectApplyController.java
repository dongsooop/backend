package com.dongsoop.dongsoop.recruitment.project.controller;

import com.dongsoop.dongsoop.recruitment.dto.RecruitmentApplyOverview;
import com.dongsoop.dongsoop.recruitment.dto.UpdateApplyStatusRequest;
import com.dongsoop.dongsoop.recruitment.project.dto.ApplyProjectBoardRequest;
import com.dongsoop.dongsoop.recruitment.project.service.ProjectApplyService;
import com.dongsoop.dongsoop.role.entity.RoleType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/project-apply")
@RequiredArgsConstructor
public class ProjectApplyController {

    private final ProjectApplyService projectApplyService;

    @GetMapping("/{boardId}")
    public ResponseEntity<List<RecruitmentApplyOverview>> getApplyById(@PathVariable @Positive Long boardId) {
        List<RecruitmentApplyOverview> overviewList = projectApplyService.getRecruitmentApplyOverview(
                boardId);

        return ResponseEntity.ok(overviewList);
    }

    @PostMapping
    @Secured(value = RoleType.USER_ROLE)
    public ResponseEntity<Void> applyProjectBoard(@RequestBody ApplyProjectBoardRequest applyProjectBoardRequest) {
        projectApplyService.apply(applyProjectBoardRequest);

        URI uri = URI.create("/project-board/" + applyProjectBoardRequest.boardId());

        return ResponseEntity.created(uri)
                .build();
    }

    @PatchMapping("/{boardId}")
    @Secured(value = RoleType.USER_ROLE)
    public ResponseEntity<Void> updateStatus(@NotNull @Positive @PathVariable Long boardId,
                                             @RequestBody @Valid UpdateApplyStatusRequest request) {
        projectApplyService.updateStatus(boardId, request);
        return ResponseEntity.noContent().build();
    }
}
