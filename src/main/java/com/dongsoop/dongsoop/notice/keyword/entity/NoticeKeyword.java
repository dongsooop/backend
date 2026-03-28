package com.dongsoop.dongsoop.notice.keyword.entity;

import com.dongsoop.dongsoop.common.BaseEntity;
import com.dongsoop.dongsoop.member.entity.Member;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "notice_keyword")
public class NoticeKeyword extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "keyword", length = 20, nullable = false)
    private String keyword;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private NoticeKeywordType type;

    public NoticeKeyword(Member member, String keyword, NoticeKeywordType type) {
        this.member = member;
        this.keyword = keyword;
        this.type = type;
    }
}