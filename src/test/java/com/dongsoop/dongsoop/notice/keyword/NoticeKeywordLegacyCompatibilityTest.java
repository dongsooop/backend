package com.dongsoop.dongsoop.notice.keyword;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.dongsoop.dongsoop.department.entity.Department;
import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.department.repository.DepartmentRepository;
import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.member.repository.MemberRepository;
import com.dongsoop.dongsoop.member.service.MemberService;
import com.dongsoop.dongsoop.memberdevice.entity.MemberDevice;
import com.dongsoop.dongsoop.memberdevice.entity.MemberDeviceType;
import com.dongsoop.dongsoop.memberdevice.repository.MemberDeviceRepository;
import com.dongsoop.dongsoop.notice.keyword.dto.NoticeKeywordRequest;
import com.dongsoop.dongsoop.notice.keyword.dto.NoticeKeywordResponse;
import com.dongsoop.dongsoop.notice.keyword.entity.NoticeKeywordType;
import com.dongsoop.dongsoop.notice.keyword.exception.DuplicateNoticeKeywordException;
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

/**
 * 기기 헤더를 보내지 않는 구버전 앱 호환 검증.
 *
 * <p>구버전 앱은 인증 토큰만 보내고 X-Device-Fid / X-Device-Token 을 붙이지 않는다.
 * 이때 키워드는 회원의 모든 기기에 일괄 적용되어야 예전처럼 "내 키워드"로 동작한다.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class NoticeKeywordLegacyCompatibilityTest {

    @Autowired
    private NoticeKeywordService noticeKeywordService;

    @Autowired
    private NoticeKeywordRepository noticeKeywordRepository;

    @Autowired
    private MemberDeviceRepository memberDeviceRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @MockitoBean
    private MemberService memberService;

    @MockitoBean
    private FCMService fcmService;

    @MockitoBean
    private BoardSearchRepository boardSearchRepository;

    @MockitoBean
    private RestaurantSearchRepository restaurantSearchRepository;

    private Member member;
    private MemberDevice phone;
    private MemberDevice tablet;

    private MemberDevice saveDevice(String suffix, Member owner) {
        MemberDevice device = MemberDevice.builder()
                .deviceToken("token-" + suffix)
                .fid("fid-" + suffix)
                .memberDeviceType(MemberDeviceType.ANDROID)
                .build();
        if (owner != null) {
            device.bindMember(owner);
        }
        return memberDeviceRepository.save(device);
    }

    @BeforeEach
    void setup() {
        Department department = departmentRepository.save(
                new Department(DepartmentType.DEPT_2001, "테스트학과", null));
        member = memberRepository.save(
                new Member(null, "legacy@dongyang.ac.kr", "구버전사용자", "password", null, department));

        phone = saveDevice("phone", member);
        tablet = saveDevice("tablet", member);
    }

    private void givenAuthenticatedMember() {
        given(memberService.getMemberIdByAuthentication()).willReturn(member.getId());
    }

    @Test
    @DisplayName("구버전 API 로 추가하면 회원의 모든 기기에 키워드가 생긴다")
    void legacyAdd_appliesToEveryDeviceOfMember() {
        givenAuthenticatedMember();

        noticeKeywordService.addKeywordByMember(
                new NoticeKeywordRequest("장학", NoticeKeywordType.INCLUDE));

        assertThat(noticeKeywordRepository.findAllByDeviceId(phone.getId()))
                .extracting(keyword -> keyword.getKeyword())
                .containsExactly("장학");
        assertThat(noticeKeywordRepository.findAllByDeviceId(tablet.getId()))
                .extracting(keyword -> keyword.getKeyword())
                .containsExactly("장학");
    }

    @Test
    @DisplayName("구버전 API 로 삭제하면 회원의 모든 기기에서 지워진다")
    void legacyDelete_removesFromEveryDeviceOfMember() {
        givenAuthenticatedMember();

        NoticeKeywordResponse added = noticeKeywordService.addKeywordByMember(
                new NoticeKeywordRequest("휴강", NoticeKeywordType.EXCLUDE));

        // 구버전 앱은 목록에서 받은 id 하나로 삭제를 요청한다
        noticeKeywordService.deleteKeywordByMember(added.id());

        assertThat(noticeKeywordRepository.findAllByDeviceId(phone.getId())).isEmpty();
        assertThat(noticeKeywordRepository.findAllByDeviceId(tablet.getId())).isEmpty();
    }

    @Test
    @DisplayName("기기마다 키워드가 달라도 구버전 조회는 합집합을 돌려준다")
    void legacyGet_returnsUnionAcrossDevices() {
        givenAuthenticatedMember();

        // 한 기기에서만 신버전으로 키워드를 추가한 상황
        noticeKeywordService.addKeyword(phone.getFid(), null,
                new NoticeKeywordRequest("장학", NoticeKeywordType.INCLUDE));
        noticeKeywordService.addKeyword(tablet.getFid(), null,
                new NoticeKeywordRequest("휴강", NoticeKeywordType.EXCLUDE));

        List<NoticeKeywordResponse> result = noticeKeywordService.getKeywordsByMember();

        assertThat(result).extracting(NoticeKeywordResponse::keyword)
                .containsExactlyInAnyOrder("장학", "휴강");
    }

    @Test
    @DisplayName("v2 API 는 지목한 기기에만 적용된다")
    void v2Add_appliesToThatDeviceOnly() {
        noticeKeywordService.addKeyword(phone.getFid(), null,
                new NoticeKeywordRequest("장학", NoticeKeywordType.INCLUDE));

        assertThat(noticeKeywordRepository.findAllByDeviceId(phone.getId())).hasSize(1);
        assertThat(noticeKeywordRepository.findAllByDeviceId(tablet.getId())).isEmpty();
    }

    @Test
    @DisplayName("같은 키워드가 이미 있으면 구버전 API 도 중복으로 막힌다")
    void legacyAdd_rejectsDuplicate() {
        givenAuthenticatedMember();

        noticeKeywordService.addKeywordByMember(
                new NoticeKeywordRequest("장학", NoticeKeywordType.INCLUDE));

        assertThatThrownBy(() -> noticeKeywordService.addKeywordByMember(
                new NoticeKeywordRequest("장학", NoticeKeywordType.INCLUDE)))
                .isInstanceOf(DuplicateNoticeKeywordException.class);
    }
}
