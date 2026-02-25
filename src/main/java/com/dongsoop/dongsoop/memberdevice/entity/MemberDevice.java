package com.dongsoop.dongsoop.memberdevice.entity;

import com.dongsoop.dongsoop.common.BaseEntity;
import com.dongsoop.dongsoop.member.entity.Member;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@SequenceGenerator(name = "member_device_sequence_generator")
public class MemberDevice extends BaseEntity {

    @Id
    @Getter
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "member_device_sequence_generator")
    private Long id;

    @Getter
    @JoinColumn(name = "member_id")
    @ManyToOne
    private Member member;

    @Getter
    @Column(nullable = false, unique = true)
    private String deviceToken;

    @Getter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MemberDeviceType memberDeviceType;

    public void bindMember(Member member) {
        this.member = member;
    }
}
