package com.dongsoop.dongsoop.notice.keyword;

import static org.assertj.core.api.Assertions.assertThat;

import com.dongsoop.dongsoop.department.entity.Department;
import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.department.repository.DepartmentRepository;
import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.member.repository.MemberRepository;
import com.dongsoop.dongsoop.notice.keyword.entity.NoticeKeyword;
import com.dongsoop.dongsoop.notice.keyword.entity.NoticeKeywordType;
import com.dongsoop.dongsoop.notice.keyword.repository.NoticeKeywordRepository;
import com.dongsoop.dongsoop.notice.keyword.service.NoticeKeywordService;
import com.dongsoop.dongsoop.notification.service.FCMService;
import com.dongsoop.dongsoop.search.repository.BoardSearchRepository;
import com.dongsoop.dongsoop.search.repository.RestaurantSearchRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class NoticeKeywordFilterTest {

    @Autowired
    private NoticeKeywordService noticeKeywordService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private NoticeKeywordRepository noticeKeywordRepository;

    @MockitoBean
    private FCMService fcmService;

    @MockitoBean
    private BoardSearchRepository boardSearchRepository;

    @MockitoBean
    private RestaurantSearchRepository restaurantSearchRepository;

    private Member memberNoKeyword;      // 키워드 없음 → 항상 수신
    private Member memberIncludeOnly;    // INCLUDE "장학" → 장학 포함 공지만 수신
    private Member memberExcludeOnly;    // EXCLUDE "휴강" → 휴강 포함 공지 제외
    private Member memberBoth;           // INCLUDE "장학" + EXCLUDE "긴급"

    @BeforeEach
    void setup() {
        Department department = departmentRepository.save(
                new Department(DepartmentType.DEPT_2001, "테스트학과", null));

        memberNoKeyword = memberRepository.save(
                new Member(null, "no-keyword@dongyang.ac.kr", "키워드없음", "password", null, department));
        memberIncludeOnly = memberRepository.save(
                new Member(null, "include@dongyang.ac.kr", "포함키워드", "password", null, department));
        memberExcludeOnly = memberRepository.save(
                new Member(null, "exclude@dongyang.ac.kr", "제외키워드", "password", null, department));
        memberBoth = memberRepository.save(
                new Member(null, "both@dongyang.ac.kr", "혼합키워드", "password", null, department));

        noticeKeywordRepository.save(new NoticeKeyword(memberIncludeOnly, "장학", NoticeKeywordType.INCLUDE));
        noticeKeywordRepository.save(new NoticeKeyword(memberExcludeOnly, "휴강", NoticeKeywordType.EXCLUDE));
        noticeKeywordRepository.save(new NoticeKeyword(memberBoth, "장학", NoticeKeywordType.INCLUDE));
        noticeKeywordRepository.save(new NoticeKeyword(memberBoth, "긴급", NoticeKeywordType.EXCLUDE));
    }

    @Test
    @DisplayName("키워드가 없는 회원은 모든 공지에 대해 알림을 받는다")
    void noKeyword_AlwaysReceives() {
        List<Member> all = List.of(memberNoKeyword, memberIncludeOnly, memberExcludeOnly, memberBoth);

        List<Member> result = noticeKeywordService.filterMembersByKeyword(all, "수강신청 일정 안내");

        assertThat(result).contains(memberNoKeyword);
    }

    @Test
    @DisplayName("INCLUDE 키워드가 있는 회원은 제목에 키워드가 포함된 공지만 받는다")
    void includeKeyword_ReceivesOnlyMatching() {
        List<Member> all = List.of(memberNoKeyword, memberIncludeOnly);

        List<Member> matchResult = noticeKeywordService.filterMembersByKeyword(all, "2025 장학생 모집 안내");
        assertThat(matchResult).contains(memberIncludeOnly);

        List<Member> noMatchResult = noticeKeywordService.filterMembersByKeyword(all, "수강신청 일정 안내");
        assertThat(noMatchResult).doesNotContain(memberIncludeOnly);
    }

    @Test
    @DisplayName("EXCLUDE 키워드가 있는 회원은 제목에 키워드가 포함된 공지를 받지 않는다")
    void excludeKeyword_FiltersOutMatching() {
        List<Member> all = List.of(memberNoKeyword, memberExcludeOnly);

        List<Member> excludedResult = noticeKeywordService.filterMembersByKeyword(all, "3주차 강의 휴강 안내");
        assertThat(excludedResult).doesNotContain(memberExcludeOnly);

        List<Member> receivedResult = noticeKeywordService.filterMembersByKeyword(all, "장학생 선발 공고");
        assertThat(receivedResult).contains(memberExcludeOnly);
    }

    @Test
    @DisplayName("INCLUDE와 EXCLUDE 모두 있는 경우 INCLUDE 매칭 AND EXCLUDE 미매칭일 때만 수신한다")
    void bothKeywords_ReceivesOnlyWhenIncludeMatchesAndExcludeDoesNot() {
        List<Member> all = List.of(memberBoth);

        // INCLUDE "장학" 매칭, EXCLUDE "긴급" 미매칭 → 수신
        List<Member> shouldReceive = noticeKeywordService.filterMembersByKeyword(all, "2025 장학생 모집");
        assertThat(shouldReceive).contains(memberBoth);

        // INCLUDE "장학" 매칭, EXCLUDE "긴급" 매칭 → 미수신
        List<Member> excludedByBoth = noticeKeywordService.filterMembersByKeyword(all, "긴급 장학 공지");
        assertThat(excludedByBoth).doesNotContain(memberBoth);

        // INCLUDE 미매칭 → 미수신
        List<Member> noIncludeMatch = noticeKeywordService.filterMembersByKeyword(all, "수강신청 일정 안내");
        assertThat(noIncludeMatch).doesNotContain(memberBoth);
    }

    @Test
    @DisplayName("대소문자를 구분하지 않고 키워드를 매칭한다")
    void keywordMatching_IsCaseInsensitive() {
        List<Member> all = List.of(memberIncludeOnly);

        List<Member> upperResult = noticeKeywordService.filterMembersByKeyword(all, "SCHOLARSHIP 장학 안내");
        assertThat(upperResult).contains(memberIncludeOnly);
    }

    @Test
    @DisplayName("대상 회원이 없으면 빈 목록을 반환한다")
    void emptyMembers_ReturnsEmptyList() {
        List<Member> result = noticeKeywordService.filterMembersByKeyword(List.of(), "장학 공지");

        assertThat(result).isEmpty();
    }
}
