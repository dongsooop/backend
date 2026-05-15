package com.dongsoop.dongsoop.eclass.controller;

import com.dongsoop.dongsoop.eclass.dto.EclassAssignmentResponse;
import com.dongsoop.dongsoop.eclass.dto.EclassAssignmentSaveRequest;
import com.dongsoop.dongsoop.eclass.service.EclassAssignmentService;
import com.dongsoop.dongsoop.member.service.MemberService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/eclass/assignments")
@RequiredArgsConstructor
public class EclassAssignmentController {

    private final EclassAssignmentService eclassAssignmentService;
    private final MemberService memberService;

    @PostMapping
    public ResponseEntity<List<EclassAssignmentResponse>> saveAssignments(
            @Valid @RequestBody EclassAssignmentSaveRequest request) {
        Long memberId = memberService.getMemberIdByAuthentication();
        List<EclassAssignmentResponse> result = eclassAssignmentService.saveAssignments(memberId, request);

        return ResponseEntity.ok(result);
    }

    @GetMapping
    public ResponseEntity<List<EclassAssignmentResponse>> getAssignments() {
        Long memberId = memberService.getMemberIdByAuthentication();
        List<EclassAssignmentResponse> result = eclassAssignmentService.getAssignments(memberId);

        return ResponseEntity.ok(result);
    }
}
