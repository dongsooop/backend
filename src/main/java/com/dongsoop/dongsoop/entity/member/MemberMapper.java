package com.dongsoop.dongsoop.entity.member;

import com.dongsoop.dongsoop.dto.member.MemberResponseDto;
import com.dongsoop.dongsoop.dto.member.MemberSignupRequestDto;
import com.dongsoop.dongsoop.dto.member.MemberUpdateRequestDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface MemberMapper {
    MemberResponseDto toResponseDto(Member member);

    @Mapping(target = "id", ignore = true)
    Member toEntity(MemberSignupRequestDto requestDto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "email", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "studentNumber", ignore = true)
    void updateMemberFromDto(MemberUpdateRequestDto dto, @MappingTarget Member member);
}