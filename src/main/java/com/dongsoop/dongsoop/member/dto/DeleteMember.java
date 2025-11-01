package com.dongsoop.dongsoop.member.dto;

public record DeleteMember(
        Long memberId,
        String passwordAlias
) {
}
