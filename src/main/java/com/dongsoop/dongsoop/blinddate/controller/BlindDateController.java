package com.dongsoop.dongsoop.blinddate.controller;

import com.dongsoop.dongsoop.blinddate.dto.StartBlindDateRequest;
import com.dongsoop.dongsoop.blinddate.service.BlindDateService;
import com.dongsoop.dongsoop.role.entity.RoleType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/blinddate")
@RequiredArgsConstructor
public class BlindDateController {

    private final BlindDateService blindDateService;

    /**
     * 과팅 운영 중인지 확인
     *
     * @return 과팅 운영 중 여부
     */
    @Secured(RoleType.USER_ROLE)
    @GetMapping
    public ResponseEntity<Boolean> isAvailable() {
        boolean available = blindDateService.isAvailable();
        return ResponseEntity.ok(available);
    }

    /**
     * 과팅 시작
     *
     * @param request 과팅 시작을 위한 정보(세션별 사용자 수, 세션 만료 시간)
     */
    @Secured(RoleType.ADMIN_ROLE)
    @PostMapping
    public ResponseEntity<Void> startBlindDate(@Valid @RequestBody StartBlindDateRequest request) {
        // 이미 운영 중이면 무시
        if (blindDateService.isAvailable()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .build();
        }

        log.info("Starting blind date: expiredDate={}, maxSessionMemberCount={}",
                request.getExpiredDate(), request.getMaxSessionMemberCount());

        blindDateService.startBlindDate(request);

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
