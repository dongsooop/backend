package com.dongsoop.dongsoop.recruitment.apply.study.controller;

import com.dongsoop.dongsoop.recruitment.apply.dto.ApplyDetails;
import com.dongsoop.dongsoop.recruitment.apply.dto.RecruitmentApplyOverview;
import com.dongsoop.dongsoop.recruitment.apply.dto.UpdateApplyStatusRequest;
import com.dongsoop.dongsoop.recruitment.apply.study.dto.ApplyStudyBoardRequest;
import com.dongsoop.dongsoop.recruitment.apply.study.service.StudyApplyService;
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
@RequestMapping("/study-apply")
@RequiredArgsConstructor
public class StudyApplyController {

    private final StudyApplyService studyApplyService;

    @GetMapping("/{boardId}")
    @Secured(RoleType.USER_ROLE)
    public ResponseEntity<List<RecruitmentApplyOverview>> getApplyById(
            @PathVariable("boardId") @Positive Long boardId) {
        List<RecruitmentApplyOverview> overviewList = studyApplyService.getRecruitmentApplyOverview(
                boardId);

        return ResponseEntity.ok(overviewList);
    }

    @GetMapping("/{boardId}/applier/{applierId}")
    @Secured(RoleType.USER_ROLE)
    public ResponseEntity<ApplyDetails> getApplyDetails(@PathVariable("boardId") @Positive Long boardId,
                                                        @PathVariable("applierId") @Positive Long applierId) {
        ApplyDetails applyDetails = studyApplyService.getRecruitmentApplyDetails(boardId, applierId);

        return ResponseEntity.ok(applyDetails);
    }

    @GetMapping("/self/{boardId}")
    @Secured(RoleType.USER_ROLE)
    public ResponseEntity<ApplyDetails> getApplyDetailsBySelf(@PathVariable("boardId") @Positive Long boardId) {
        ApplyDetails applyDetails = studyApplyService.getRecruitmentApplyDetails(boardId);

        return ResponseEntity.ok(applyDetails);
    }

    @PostMapping
    @Secured(value = RoleType.USER_ROLE)
    public ResponseEntity<Void> applyStudyBoard(@RequestBody @Valid ApplyStudyBoardRequest request) {
        studyApplyService.apply(request);

        URI uri = URI.create("/study-board/" + request.boardId());

        return ResponseEntity.created(uri)
                .build();
    }

    @PatchMapping("/{boardId}")
    @Secured(value = RoleType.USER_ROLE)
    public ResponseEntity<Void> updateStatus(@NotNull @Positive @PathVariable Long boardId,
                                             @RequestBody @Valid UpdateApplyStatusRequest request) {
        studyApplyService.updateStatus(boardId, request);
        return ResponseEntity.noContent().build();
    }
}
