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
import jakarta.persistence.PrePersist;
import jakarta.persistence.SequenceGenerator;
import java.time.LocalDateTime;
import java.util.UUID;
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
    @Column(unique = true)
    private String deviceToken;

    @Getter
    @Column(name = "anonymous_key", length = 36, unique = true)
    private String anonymousKey;

    @Getter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MemberDeviceType memberDeviceType;

    public void bindMember(Member member) {
        this.member = member;
    }

    @Column
    private LocalDateTime lastAccess;

    @PrePersist
    protected void initLastAccess() {
        if (this.lastAccess == null) {
            this.lastAccess = LocalDateTime.now();
        }
    }

    public void updateDeviceToken(String deviceToken) {
        this.deviceToken = deviceToken;
    }

    /**
     * 비회원 식별용 익명 키를 발급한다.
     *
     * <p>이미 발급된 키가 있으면 재발급하지 않고 기존 값을 반환한다.
     * 앱이 보관 중인 키를 무효화하지 않기 위함이다.
     */
    public String issueAnonymousKeyIfAbsent() {
        if (this.anonymousKey == null) {
            this.anonymousKey = UUID.randomUUID().toString();
        }

        return this.anonymousKey;
    }

    public void updateLastAccess(LocalDateTime lastAccess) {
        this.lastAccess = lastAccess;
    }
}
