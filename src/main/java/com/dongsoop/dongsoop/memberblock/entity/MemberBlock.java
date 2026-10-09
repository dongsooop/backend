package com.dongsoop.dongsoop.memberblock.entity;

import com.dongsoop.dongsoop.member.entity.Member;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Entity
@IdClass(MemberBlockId.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class MemberBlock {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blocker_id")
    private Member blocker;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blocked_member_id")
    private Member blockedMember;
}
