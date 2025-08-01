package com.dongsoop.dongsoop.recruitment.apply.tutoring.controller;

import com.dongsoop.dongsoop.recruitment.apply.dto.ApplyDetails;
import com.dongsoop.dongsoop.recruitment.apply.dto.RecruitmentApplyOverview;
import com.dongsoop.dongsoop.recruitment.apply.dto.UpdateApplyStatusRequest;
import com.dongsoop.dongsoop.recruitment.apply.tutoring.dto.ApplyTutoringBoardRequest;
import com.dongsoop.dongsoop.recruitment.apply.tutoring.service.TutoringApplyService;
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
    public ResponseEntity<List<RecruitmentApplyOverview>> getApplyById(
            @PathVariable("boardId") @Positive Long boardId) {
        List<RecruitmentApplyOverview> overviewList = tutoringApplyService.getRecruitmentApplyOverview(
                boardId);

        return ResponseEntity.ok(overviewList);
    }

    @GetMapping("/{boardId}/applier/{applierId}")
    @Secured(RoleType.USER_ROLE)
    public ResponseEntity<ApplyDetails> getApplyDetails(@PathVariable("boardId") @Positive Long boardId,
                                                        @PathVariable("applierId") @Positive Long applierId) {
        ApplyDetails applyDetails = tutoringApplyService.getRecruitmentApplyDetails(boardId, applierId);

        return ResponseEntity.ok(applyDetails);
    }

    @GetMapping("/self/{boardId}")
    @Secured(RoleType.USER_ROLE)
    public ResponseEntity<ApplyDetails> getApplyDetailsBySelf(@PathVariable("boardId") @Positive Long boardId) {
        ApplyDetails applyDetails = tutoringApplyService.getRecruitmentApplyDetails(boardId);

        return ResponseEntity.ok(applyDetails);
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
