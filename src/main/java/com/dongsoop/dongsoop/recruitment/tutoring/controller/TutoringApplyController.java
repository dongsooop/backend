package com.dongsoop.dongsoop.recruitment.tutoring.controller;

import com.dongsoop.dongsoop.recruitment.dto.RecruitmentApplyOverview;
import com.dongsoop.dongsoop.recruitment.dto.UpdateApplyStatusRequest;
import com.dongsoop.dongsoop.recruitment.tutoring.dto.ApplyTutoringBoardRequest;
import com.dongsoop.dongsoop.recruitment.tutoring.service.TutoringApplyService;
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
@RequestMapping("/tutoring-apply")
@RequiredArgsConstructor
public class TutoringApplyController {

    private final TutoringApplyService tutoringApplyService;

    @GetMapping("/{boardId}")
    @Secured(RoleType.USER_ROLE)
    public ResponseEntity<List<RecruitmentApplyOverview>> getApplyById(@PathVariable @Positive Long boardId) {
        List<RecruitmentApplyOverview> overviewList = tutoringApplyService.getRecruitmentApplyOverview(
                boardId);

        return ResponseEntity.ok(overviewList);
    }

    @PostMapping
    @Secured(value = RoleType.USER_ROLE)
    public ResponseEntity<Void> applyTutoringBoard(@RequestBody ApplyTutoringBoardRequest request) {
        tutoringApplyService.apply(request);

        URI uri = URI.create("/tutoring-board/" + request.boardId());

        return ResponseEntity.created(uri)
                .build();
    }

    @PatchMapping("/{boardId}")
    @Secured(value = RoleType.USER_ROLE)
    public ResponseEntity<Void> updateStatus(@NotNull @Positive @PathVariable Long boardId,
                                             @RequestBody @Valid UpdateApplyStatusRequest request) {
        tutoringApplyService.updateStatus(boardId, request);
        return ResponseEntity.noContent().build();
    }
}
