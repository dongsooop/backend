package com.dongsoop.dongsoop.notice.keyword;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dongsoop.dongsoop.memberdevice.entity.MemberDevice;
import com.dongsoop.dongsoop.memberdevice.entity.MemberDeviceType;
import com.dongsoop.dongsoop.memberdevice.repository.MemberDeviceRepository;
import com.dongsoop.dongsoop.notice.keyword.dto.NoticeKeywordRequest;
import com.dongsoop.dongsoop.notice.keyword.dto.NoticeKeywordResponse;
import com.dongsoop.dongsoop.notice.keyword.entity.NoticeKeywordType;
import com.dongsoop.dongsoop.notice.keyword.exception.DuplicateNoticeKeywordException;
import com.dongsoop.dongsoop.notice.keyword.exception.NoticeKeywordNotFoundException;
import com.dongsoop.dongsoop.notice.keyword.service.GuestNoticeKeywordService;
import com.dongsoop.dongsoop.notification.service.FCMService;
import com.dongsoop.dongsoop.search.repository.BoardSearchRepository;
import com.dongsoop.dongsoop.search.repository.RestaurantSearchRepository;
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
class GuestNoticeKeywordTest {

    @Autowired
    private GuestNoticeKeywordService service;

    @Autowired
    private MemberDeviceRepository memberDeviceRepository;

    @MockitoBean
    private FCMService fcmService;

    @MockitoBean
    private BoardSearchRepository boardSearchRepository;

    @MockitoBean
    private RestaurantSearchRepository restaurantSearchRepository;

    private String saveGuestDeviceKey(String token) {
        MemberDevice device = MemberDevice.builder()
                .deviceToken(token)
                .memberDeviceType(MemberDeviceType.ANDROID)
                .build();
        device.issueAnonymousKeyIfAbsent();
        memberDeviceRepository.save(device);

        return device.getAnonymousKey();
    }

    @Test
    @DisplayName("비회원이 추가한 키워드가 목록에 나온다")
    void adds_and_lists_keyword() {
        String key = saveGuestDeviceKey("token-kw-1");

        service.addKeyword(key, new NoticeKeywordRequest("장학", NoticeKeywordType.INCLUDE));

        assertThat(service.getKeywords(key))
                .extracting(NoticeKeywordResponse::keyword)
                .containsExactly("장학");
    }

    @Test
    @DisplayName("같은 키워드를 같은 타입으로 두 번 추가하면 예외를 던진다")
    void rejects_duplicate() {
        String key = saveGuestDeviceKey("token-kw-2");
        service.addKeyword(key, new NoticeKeywordRequest("장학", NoticeKeywordType.INCLUDE));

        assertThatThrownBy(() -> service.addKeyword(key, new NoticeKeywordRequest("장학", NoticeKeywordType.INCLUDE)))
                .isInstanceOf(DuplicateNoticeKeywordException.class);
    }

    @Test
    @DisplayName("다른 디바이스의 키워드는 삭제할 수 없다")
    void cannot_delete_other_device_keyword() {
        String ownerKey = saveGuestDeviceKey("token-kw-3");
        String otherKey = saveGuestDeviceKey("token-kw-4");
        NoticeKeywordResponse created = service.addKeyword(ownerKey,
                new NoticeKeywordRequest("장학", NoticeKeywordType.INCLUDE));

        assertThatThrownBy(() -> service.deleteKeyword(otherKey, created.id()))
                .isInstanceOf(NoticeKeywordNotFoundException.class);
    }

    @Test
    @DisplayName("자기 키워드는 삭제된다")
    void deletes_own_keyword() {
        String key = saveGuestDeviceKey("token-kw-5");
        NoticeKeywordResponse created = service.addKeyword(key,
                new NoticeKeywordRequest("장학", NoticeKeywordType.INCLUDE));

        service.deleteKeyword(key, created.id());

        assertThat(service.getKeywords(key)).isEmpty();
    }
}
