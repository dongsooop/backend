package com.dongsoop.dongsoop.notice.preference.controller;

import com.dongsoop.dongsoop.notice.preference.dto.GuestDepartmentRequest;
import com.dongsoop.dongsoop.notice.preference.dto.GuestDepartmentResponse;
import com.dongsoop.dongsoop.notice.preference.service.GuestNoticePreferenceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/guest/department")
@RequiredArgsConstructor
public class GuestNoticePreferenceController {

    private final GuestNoticePreferenceService guestNoticePreferenceService;

    @PutMapping
    public ResponseEntity<Void> updateDepartment(
            @RequestHeader(value = "X-Anonymous-Key", required = false) String anonymousKey,
            @RequestBody @Valid GuestDepartmentRequest request) {
        guestNoticePreferenceService.updateDepartment(anonymousKey, request.departmentType());

        return ResponseEntity.noContent()
                .build();
    }

    @GetMapping
    public ResponseEntity<GuestDepartmentResponse> getDepartment(
            @RequestHeader(value = "X-Anonymous-Key", required = false) String anonymousKey) {
        return ResponseEntity.ok(guestNoticePreferenceService.getDepartment(anonymousKey));
    }
}
