package com.dongsoop.dongsoop.notice.preference.controller;

import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.notice.preference.dto.SubscribeDepartmentRequest;
import com.dongsoop.dongsoop.notice.preference.dto.SubscribeDepartmentResponse;
import com.dongsoop.dongsoop.notice.preference.service.GuestNoticePreferenceService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/subscribe-department")
@RequiredArgsConstructor
public class SubscribeDepartmentController {

    private final GuestNoticePreferenceService guestNoticePreferenceService;

    @PutMapping
    public ResponseEntity<Void> updateDepartments(
            @RequestHeader(value = "X-Device-Fid", required = false) String fid,
            @RequestHeader(value = "X-Device-Token", required = false) String deviceToken,
            @RequestBody @Valid SubscribeDepartmentRequest request) {
        guestNoticePreferenceService.updateDepartments(fid, deviceToken, request.departmentTypes());

        return ResponseEntity.noContent()
                .build();
    }

    @GetMapping
    public ResponseEntity<SubscribeDepartmentResponse> getDepartments(
            @RequestHeader(value = "X-Device-Fid", required = false) String fid,
            @RequestHeader(value = "X-Device-Token", required = false) String deviceToken) {
        List<DepartmentType> departmentTypes = guestNoticePreferenceService.getDepartmentTypes(fid, deviceToken);
        SubscribeDepartmentResponse response = new SubscribeDepartmentResponse(departmentTypes);

        return ResponseEntity.ok(response);
    }
}
