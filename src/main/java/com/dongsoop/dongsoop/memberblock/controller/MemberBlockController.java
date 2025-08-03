package com.dongsoop.dongsoop.memberblock.controller;

import com.dongsoop.dongsoop.memberblock.dto.MemberBlockRequest;
import com.dongsoop.dongsoop.memberblock.service.MemberBlockService;
import com.dongsoop.dongsoop.role.entity.RoleType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/member-block")
@RequiredArgsConstructor
public class MemberBlockController {

    private final MemberBlockService memberBlockService;

    @PostMapping
    @Secured(RoleType.USER_ROLE)
    public ResponseEntity<Void> blocked(@RequestBody MemberBlockRequest request) {
        memberBlockService.blockMember(request);

        return ResponseEntity.noContent()
                .build();
    }
}
