package com.dongsoop.dongsoop.memberblock.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class BlockedMember {

    Long blockedMemberId;
    String memberName;
}
