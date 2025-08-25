package com.dongsoop.dongsoop.memberdevice.controller;

import com.dongsoop.dongsoop.memberdevice.dto.DeviceRegisterRequest;
import com.dongsoop.dongsoop.memberdevice.service.MemberDeviceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/device")
@RequiredArgsConstructor
public class MemberDeviceController {

    private final MemberDeviceService memberDeviceService;

    @PostMapping
    public ResponseEntity<Void> registerDevice(@RequestBody @Valid DeviceRegisterRequest request) {
        memberDeviceService.registerDevice(request.deviceToken(), request.type());

        return ResponseEntity.noContent()
                .build();
    }
}
