package com.dongsoop.dongsoop.member.dto;

import com.dongsoop.dongsoop.member.entity.Role;

public interface MemberDetailsDto {
    Long getId();
    String getPassword();
    Role getRole();
}