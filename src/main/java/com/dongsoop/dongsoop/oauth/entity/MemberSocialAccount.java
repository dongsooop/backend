package com.dongsoop.dongsoop.oauth.entity;

import com.dongsoop.dongsoop.member.entity.Member;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor
@AllArgsConstructor
public class MemberSocialAccount {

    @EmbeddedId
    private MemberSocialAccountId id;

    @Getter
    @JoinColumn(name = "member_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private Member member;

    @Getter
    @Column(name = "create_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public MemberSocialAccount(MemberSocialAccountId id, Member member) {
        this.id = id;
        this.member = member;
    }

    @PrePersist
    private void prePersist() {
        if (this.createAt == null) {
            this.createAt = LocalDateTime.now();
        }
    }

    public void updateMember(Member member) {
        this.member = member;
    }
}
