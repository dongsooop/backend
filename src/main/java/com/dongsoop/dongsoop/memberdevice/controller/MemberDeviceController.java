package com.dongsoop.dongsoop.memberdevice.controller;

import com.dongsoop.dongsoop.memberdevice.dto.DeviceRegisterRequest;
import com.dongsoop.dongsoop.memberdevice.service.MemberDeviceService;
import com.dongsoop.dongsoop.notification.service.FCMService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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
    private final FCMService fcmService;

    @Value("${notification.topic.anonymous}")
    private String anonymousTopic;

    @PostMapping
    public ResponseEntity<Void> registerDevice(@RequestBody @Valid DeviceRegisterRequest request) {
        memberDeviceService.registerDevice(request.deviceToken(), request.type());
        fcmService.subscribeTopic(List.of(anonymousTopic), anonymousTopic);

        return ResponseEntity.noContent()
                .build();
    }
}
