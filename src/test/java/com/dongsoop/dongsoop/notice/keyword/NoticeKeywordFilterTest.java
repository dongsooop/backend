package com.dongsoop.dongsoop.notice.keyword;

import static org.assertj.core.api.Assertions.assertThat;

import com.dongsoop.dongsoop.memberdevice.entity.MemberDevice;
import com.dongsoop.dongsoop.memberdevice.entity.MemberDeviceType;
import com.dongsoop.dongsoop.memberdevice.repository.MemberDeviceRepository;
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
    private MemberDeviceRepository memberDeviceRepository;

    @Autowired
    private NoticeKeywordRepository noticeKeywordRepository;

    @MockitoBean
    private FCMService fcmService;

    @MockitoBean
    private BoardSearchRepository boardSearchRepository;

    @MockitoBean
    private RestaurantSearchRepository restaurantSearchRepository;

    private MemberDevice deviceNoKeyword;      // 키워드 없음 → 항상 수신
    private MemberDevice deviceIncludeOnly;    // INCLUDE "장학" → 장학 포함 공지만 수신
    private MemberDevice deviceExcludeOnly;    // EXCLUDE "휴강" → 휴강 포함 공지 제외
    private MemberDevice deviceBoth;           // INCLUDE "장학" + EXCLUDE "긴급"

    /**
     * 키워드는 기기 단위이므로 회원 바인딩 없이도 성립한다. 여기서는 member 를 붙이지 않은
     * 기기(= 비회원이거나 로그아웃한 기기)로 검증해, 회원 여부와 무관하게 필터가 도는 것까지 함께 확인한다.
     */
    private MemberDevice saveDevice(String suffix) {
        return memberDeviceRepository.save(MemberDevice.builder()
                .deviceToken("token-" + suffix)
                .fid("fid-" + suffix)
                .memberDeviceType(MemberDeviceType.ANDROID)
                .build());
    }

    /** 테스트마다 대상 기기가 다르므로 그 목록으로 필터를 만들어 바로 적용한다. */
    private List<MemberDevice> filter(List<MemberDevice> devices, String noticeTitle) {
        return noticeKeywordService.loadFilter(devices).apply(devices, noticeTitle);
    }

    @BeforeEach
    void setup() {
        deviceNoKeyword = saveDevice("no-keyword");
        deviceIncludeOnly = saveDevice("include");
        deviceExcludeOnly = saveDevice("exclude");
        deviceBoth = saveDevice("both");

        noticeKeywordRepository.save(new NoticeKeyword(deviceIncludeOnly, "장학", NoticeKeywordType.INCLUDE));
        noticeKeywordRepository.save(new NoticeKeyword(deviceExcludeOnly, "휴강", NoticeKeywordType.EXCLUDE));
        noticeKeywordRepository.save(new NoticeKeyword(deviceBoth, "장학", NoticeKeywordType.INCLUDE));
        noticeKeywordRepository.save(new NoticeKeyword(deviceBoth, "긴급", NoticeKeywordType.EXCLUDE));
    }

    @Test
    @DisplayName("키워드가 없는 기기는 모든 공지에 대해 알림을 받는다")
    void noKeyword_AlwaysReceives() {
        List<MemberDevice> all = List.of(deviceNoKeyword, deviceIncludeOnly, deviceExcludeOnly, deviceBoth);

        List<MemberDevice> result = filter(all, "수강신청 일정 안내");

        assertThat(result).contains(deviceNoKeyword);
    }

    @Test
    @DisplayName("INCLUDE 키워드가 있는 기기는 제목에 키워드가 포함된 공지만 받는다")
    void includeKeyword_ReceivesOnlyMatching() {
        List<MemberDevice> all = List.of(deviceNoKeyword, deviceIncludeOnly);

        List<MemberDevice> matchResult = filter(all, "2025 장학생 모집 안내");
        assertThat(matchResult).contains(deviceIncludeOnly);

        List<MemberDevice> noMatchResult = filter(all, "수강신청 일정 안내");
        assertThat(noMatchResult).doesNotContain(deviceIncludeOnly);
    }

    @Test
    @DisplayName("EXCLUDE 키워드가 있는 기기는 제목에 키워드가 포함된 공지를 받지 않는다")
    void excludeKeyword_FiltersOutMatching() {
        List<MemberDevice> all = List.of(deviceNoKeyword, deviceExcludeOnly);

        List<MemberDevice> excludedResult = filter(all, "3주차 강의 휴강 안내");
        assertThat(excludedResult).doesNotContain(deviceExcludeOnly);

        List<MemberDevice> receivedResult = filter(all, "장학생 선발 공고");
        assertThat(receivedResult).contains(deviceExcludeOnly);
    }

    @Test
    @DisplayName("INCLUDE와 EXCLUDE 모두 있는 경우 INCLUDE 매칭 AND EXCLUDE 미매칭일 때만 수신한다")
    void bothKeywords_ReceivesOnlyWhenIncludeMatchesAndExcludeDoesNot() {
        List<MemberDevice> all = List.of(deviceBoth);

        // INCLUDE "장학" 매칭, EXCLUDE "긴급" 미매칭 → 수신
        List<MemberDevice> shouldReceive = filter(all, "2025 장학생 모집");
        assertThat(shouldReceive).contains(deviceBoth);

        // INCLUDE "장학" 매칭, EXCLUDE "긴급" 매칭 → 미수신
        List<MemberDevice> excludedByBoth = filter(all, "긴급 장학 공지");
        assertThat(excludedByBoth).doesNotContain(deviceBoth);

        // INCLUDE 미매칭 → 미수신
        List<MemberDevice> noIncludeMatch = filter(all, "수강신청 일정 안내");
        assertThat(noIncludeMatch).doesNotContain(deviceBoth);
    }

    @Test
    @DisplayName("대소문자를 구분하지 않고 키워드를 매칭한다")
    void keywordMatching_IsCaseInsensitive() {
        List<MemberDevice> all = List.of(deviceIncludeOnly);

        List<MemberDevice> upperResult = filter(all, "SCHOLARSHIP 장학 안내");
        assertThat(upperResult).contains(deviceIncludeOnly);
    }

    @Test
    @DisplayName("대상 기기가 없으면 빈 목록을 반환한다")
    void emptyDevices_ReturnsEmptyList() {
        List<MemberDevice> result = filter(List.of(), "장학 공지");

        assertThat(result).isEmpty();
    }
}
