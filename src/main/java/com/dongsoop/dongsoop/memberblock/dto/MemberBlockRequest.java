package com.dongsoop.dongsoop.memberblock.dto;

public record MemberBlockRequest(

        Long blockerId,
        Long blockedMemberId
) {
}
