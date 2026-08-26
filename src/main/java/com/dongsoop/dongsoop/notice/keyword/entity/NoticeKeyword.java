package com.dongsoop.dongsoop.notice.keyword.entity;

import com.dongsoop.dongsoop.common.BaseEntity;
import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.memberdevice.entity.MemberDevice;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SequenceGenerator(name = "notice_keyword_sequence_generator")
@Table(name = "notice_keyword")
public class NoticeKeyword extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "notice_keyword_sequence_generator")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    // 기기 정리 스케줄러는 member_device 를 벌크 삭제하므로, DB 레벨에서 함께 지워지지 않으면 FK 위반으로 정리가 통째로 롤백된다
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_device_id", foreignKey = @ForeignKey(
            name = "fk_notice_keyword_member_device",
            foreignKeyDefinition = "FOREIGN KEY (member_device_id) REFERENCES member_device (id) ON DELETE CASCADE"))
    private MemberDevice device;

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

    public NoticeKeyword(MemberDevice device, String keyword, NoticeKeywordType type) {
        this.device = device;
        this.keyword = keyword;
        this.type = type;
    }
}