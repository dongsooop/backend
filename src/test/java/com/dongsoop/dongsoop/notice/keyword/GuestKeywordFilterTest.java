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
class GuestKeywordFilterTest {

    @Autowired
    private NoticeKeywordService noticeKeywordService;

    @Autowired
    private NoticeKeywordRepository noticeKeywordRepository;

    @Autowired
    private MemberDeviceRepository memberDeviceRepository;

    @MockitoBean
    private FCMService fcmService;

    @MockitoBean
    private BoardSearchRepository boardSearchRepository;

    @MockitoBean
    private RestaurantSearchRepository restaurantSearchRepository;

    private MemberDevice saveDevice(String token) {
        MemberDevice device = MemberDevice.builder()
                .deviceToken(token)
                .memberDeviceType(MemberDeviceType.ANDROID)
                .build();
        device.issueAnonymousKeyIfAbsent();

        return memberDeviceRepository.save(device);
    }

    @Test
    @DisplayName("키워드가 없는 디바이스는 모두 통과한다")
    void passes_device_without_keyword() {
        MemberDevice device = saveDevice("token-f-1");

        List<MemberDevice> result = noticeKeywordService.filterDevicesByKeyword(List.of(device), "장학금 안내");

        assertThat(result).containsExactly(device);
    }

    @Test
    @DisplayName("INCLUDE 키워드가 제목에 없으면 제외된다")
    void filters_out_when_include_not_matched() {
        MemberDevice device = saveDevice("token-f-2");
        noticeKeywordRepository.save(new NoticeKeyword(device, "장학", NoticeKeywordType.INCLUDE));

        List<MemberDevice> result = noticeKeywordService.filterDevicesByKeyword(List.of(device), "휴강 안내");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("EXCLUDE 키워드가 제목에 있으면 제외된다")
    void filters_out_when_exclude_matched() {
        MemberDevice device = saveDevice("token-f-3");
        noticeKeywordRepository.save(new NoticeKeyword(device, "휴강", NoticeKeywordType.EXCLUDE));

        List<MemberDevice> result = noticeKeywordService.filterDevicesByKeyword(List.of(device), "휴강 안내");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("빈 목록을 넣으면 빈 목록이 나온다")
    void returns_empty_for_empty_input() {
        assertThat(noticeKeywordService.filterDevicesByKeyword(List.of(), "장학금 안내")).isEmpty();
    }
}
