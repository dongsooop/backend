package com.dongsoop.dongsoop.eclass.controller;

import com.dongsoop.dongsoop.eclass.dto.EclassAssignmentListResponse;
import com.dongsoop.dongsoop.eclass.dto.EclassLinkRequest;
import com.dongsoop.dongsoop.eclass.dto.EclassLinkResponse;
import com.dongsoop.dongsoop.eclass.service.EclassAssignmentService;
import com.dongsoop.dongsoop.eclass.service.EclassLinkService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 이클래스 연동은 회원/비회원 구분 없이 기기 단위로 동작한다 — 학과 구독과 같은 헤더로 기기를 식별한다.
 */
@RestController
@RequestMapping("/eclass")
@RequiredArgsConstructor
public class EclassController {

    private final EclassLinkService eclassLinkService;
    private final EclassAssignmentService eclassAssignmentService;

    @PostMapping("/link")
    public ResponseEntity<EclassLinkResponse> link(
            @RequestHeader(value = "X-Device-Fid", required = false) String fid,
            @RequestHeader(value = "X-Device-Token", required = false) String deviceToken,
            @RequestBody @Valid EclassLinkRequest request) {
        EclassLinkResponse response = eclassLinkService.link(fid, deviceToken, request.token());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/link")
    public ResponseEntity<EclassLinkResponse> getLink(
            @RequestHeader(value = "X-Device-Fid", required = false) String fid,
            @RequestHeader(value = "X-Device-Token", required = false) String deviceToken) {
        return ResponseEntity.ok(eclassLinkService.getStatus(fid, deviceToken));
    }

    @DeleteMapping("/link")
    public ResponseEntity<Void> unlink(
            @RequestHeader(value = "X-Device-Fid", required = false) String fid,
            @RequestHeader(value = "X-Device-Token", required = false) String deviceToken) {
        eclassLinkService.unlink(fid, deviceToken);

        return ResponseEntity.noContent()
                .build();
    }

    @GetMapping("/assignments")
    public ResponseEntity<EclassAssignmentListResponse> getAssignments(
            @RequestHeader(value = "X-Device-Fid", required = false) String fid,
            @RequestHeader(value = "X-Device-Token", required = false) String deviceToken) {
        return ResponseEntity.ok(eclassAssignmentService.getUpcoming(fid, deviceToken));
    }

    @PostMapping("/sync")
    public ResponseEntity<Void> sync(
            @RequestHeader(value = "X-Device-Fid", required = false) String fid,
            @RequestHeader(value = "X-Device-Token", required = false) String deviceToken) {
        eclassLinkService.syncNow(fid, deviceToken);

        return ResponseEntity.noContent()
                .build();
    }
}
