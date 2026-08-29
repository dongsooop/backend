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
@RequestMapping("/guest/departments")
@RequiredArgsConstructor
public class GuestNoticePreferenceController {

    private final GuestNoticePreferenceService guestNoticePreferenceService;

    @PutMapping
    public ResponseEntity<Void> updateDepartments(
            @RequestHeader(value = "X-Device-Token", required = false) String deviceToken,
            @RequestBody @Valid GuestDepartmentRequest request) {
        guestNoticePreferenceService.updateDepartments(deviceToken, request.departmentTypes());

        return ResponseEntity.noContent()
                .build();
    }

    @GetMapping
    public ResponseEntity<GuestDepartmentResponse> getDepartments(
            @RequestHeader(value = "X-Device-Token", required = false) String deviceToken) {
        return ResponseEntity.ok(new GuestDepartmentResponse(guestNoticePreferenceService.getDepartmentTypes(deviceToken)));
    }
}
