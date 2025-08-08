package com.dongsoop.dongsoop.memberdevice.entity;

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
import lombok.Builder;
import lombok.NoArgsConstructor;

@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@SequenceGenerator(name = "member_device_sequence_generator")
public class MemberDevice {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "member_device_sequence_generator")
    private Long id;

    @JoinColumn(name = "member_id", nullable = false)
    @ManyToOne
    private Member member;

    @Column(nullable = false, unique = true)
    private String deviceToken;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MemberDeviceType memberDeviceType;
}
