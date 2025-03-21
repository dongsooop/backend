package com.dongsoop.dongsoop.controller.member;

import com.dongsoop.dongsoop.dto.member.MemberLoginRequestDto;
import com.dongsoop.dongsoop.dto.member.MemberResponseDto;
import com.dongsoop.dongsoop.dto.member.MemberSignupRequestDto;
import com.dongsoop.dongsoop.dto.member.MemberUpdateRequestDto;
import com.dongsoop.dongsoop.service.member.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @PostMapping
    public ResponseEntity<MemberResponseDto> signup(@RequestBody MemberSignupRequestDto requestDto) {
        MemberResponseDto responseDto = memberService.signup(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
    }

    @PostMapping("/login")
    public ResponseEntity<MemberResponseDto> login(@RequestBody MemberLoginRequestDto requestDto) {
        MemberResponseDto responseDto = memberService.login(
                requestDto.getEmail(),
                requestDto.getPassword()
        );
        return ResponseEntity.ok(responseDto);
    }

    @GetMapping("/{id}")
    public ResponseEntity<MemberResponseDto> getMemberById(@PathVariable Long id) {
        MemberResponseDto responseDto = memberService.findById(id);
        return ResponseEntity.ok(responseDto);
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<MemberResponseDto> getMemberByEmail(@PathVariable String email) {
        MemberResponseDto responseDto = memberService.findByEmail(email);
        return ResponseEntity.ok(responseDto);
    }

    @PutMapping("/{id}")
    public ResponseEntity<MemberResponseDto> updateMember(
            @PathVariable Long id,
            @RequestBody MemberUpdateRequestDto requestDto) {
        MemberResponseDto responseDto = memberService.updateMember(id, requestDto);
        return ResponseEntity.ok(responseDto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMember(@PathVariable Long id) {
        memberService.deleteMember(id);
        return ResponseEntity.noContent().build();
    }
}