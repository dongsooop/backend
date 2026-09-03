package com.dongsoop.dongsoop.eclass;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dongsoop.dongsoop.common.exception.authentication.NotAuthenticationException;
import com.dongsoop.dongsoop.eclass.entity.EclassLink;
import com.dongsoop.dongsoop.eclass.repository.EclassAssignmentRepository;
import com.dongsoop.dongsoop.eclass.repository.EclassLinkRepository;
import com.dongsoop.dongsoop.eclass.service.EclassDeviceAccessor;
import com.dongsoop.dongsoop.eclass.service.EclassLinkServiceImpl;
import com.dongsoop.dongsoop.eclass.service.EclassSyncService;
import com.dongsoop.dongsoop.eclass.util.EclassClient;
import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.member.service.MemberService;
import com.dongsoop.dongsoop.memberdevice.entity.MemberDevice;
import com.dongsoop.dongsoop.memberdevice.exception.UnregisteredDeviceException;
import com.dongsoop.dongsoop.memberdevice.service.NoticePreferenceDeviceResolver;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * 연동 엔드포인트도 과제 조회와 같은 규칙을 따라야 한다 — 남의 기기 식별자로
 * 연동 정보를 읽거나, 지우거나, 토큰을 덮어쓸 수 없어야 한다.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EclassLinkAccessTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 1, 10, 0);
    private static final Long OWNER_ID = 1L;
    private static final Long OTHER_ID = 2L;

    @Mock
    private NoticePreferenceDeviceResolver deviceResolver;
    @Mock
    private MemberService memberService;
    @Mock
    private EclassLinkRepository linkRepository;
    @Mock
    private EclassAssignmentRepository assignmentRepository;
    @Mock
    private EclassClient eclassClient;
    @Mock
    private TextEncryptor eclassTokenEncryptor;
    @Mock
    private EclassSyncService syncService;

    private EclassLinkServiceImpl linkService;
    private MemberDevice memberDevice;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW.toInstant(ZoneOffset.ofHours(9)), ZoneId.of("Asia/Seoul"));
        EclassDeviceAccessor deviceAccessor = new EclassDeviceAccessor(deviceResolver, memberService);
        linkService = new EclassLinkServiceImpl(deviceAccessor, linkRepository, assignmentRepository,
                eclassClient, eclassTokenEncryptor, syncService, clock);
        ReflectionTestUtils.setField(linkService, "manualCooldownSeconds", 60L);

        Member owner = Member.builder().id(OWNER_ID).build();
        memberDevice = MemberDevice.builder()
                .id(10L)
                .deviceToken("fcm-token")
                .member(owner)
                .build();
        when(deviceResolver.resolve(any(), any())).thenReturn(memberDevice);

        EclassLink link = new EclassLink(memberDevice, "테스트", "encrypted");
        when(linkRepository.findByDeviceId(10L)).thenReturn(Optional.of(link));
    }

    @Test
    @DisplayName("다른 회원의 기기 식별자로는 연동 정보를 읽을 수 없다")
    void otherMemberCannotReadLink() {
        when(memberService.getMemberIdByAuthentication()).thenReturn(OTHER_ID);

        assertThat(linkService.getStatus("fid-1", null).linked()).isFalse();
    }

    @Test
    @DisplayName("다른 회원의 기기 식별자로는 연동을 해제할 수 없다")
    void otherMemberCannotUnlink() {
        when(memberService.getMemberIdByAuthentication()).thenReturn(OTHER_ID);

        linkService.unlink("fid-1", null);

        verify(linkRepository, never()).delete(any());
        verify(assignmentRepository, never()).deleteAllByLinkId(anyLong());
    }

    @Test
    @DisplayName("로그인하지 않으면 회원 기기에 토큰을 덮어쓸 수 없고, 학교 서버도 호출하지 않는다")
    void anonymousCannotOverwriteToken() {
        when(memberService.getMemberIdByAuthentication()).thenThrow(new NotAuthenticationException());

        assertThatThrownBy(() -> linkService.link("fid-1", null, "stolen-token"))
                .isInstanceOf(UnregisteredDeviceException.class);

        verify(eclassClient, never()).getSiteInfo(any());
        verify(linkRepository, never()).save(any());
    }

    @Test
    @DisplayName("주인이면 연동 정보를 읽을 수 있다")
    void ownerCanReadLink() {
        when(memberService.getMemberIdByAuthentication()).thenReturn(OWNER_ID);

        assertThat(linkService.getStatus("fid-1", null).linked()).isTrue();
    }

    @Test
    @DisplayName("비회원 기기는 로그인 없이도 자기 연동을 다룰 수 있다")
    void guestDeviceCanManageOwnLink() {
        MemberDevice guestDevice = MemberDevice.builder()
                .id(20L)
                .deviceToken("guest-token")
                .build();
        when(deviceResolver.resolve(any(), any())).thenReturn(guestDevice);
        when(linkRepository.findByDeviceId(20L))
                .thenReturn(Optional.of(new EclassLink(guestDevice, "테스트", "encrypted")));
        when(memberService.getMemberIdByAuthentication()).thenThrow(new NotAuthenticationException());

        assertThat(linkService.getStatus("fid-2", null).linked()).isTrue();
    }

    @Test
    @DisplayName("만료된 연동은 수동 동기화해도 학교 서버를 부르지 않는다")
    void manualSyncSkipsExpiredLink() {
        when(memberService.getMemberIdByAuthentication()).thenReturn(OWNER_ID);
        EclassLink expired = new EclassLink(memberDevice, "테스트", "encrypted");
        expired.expire(NOW);
        when(linkRepository.findByDeviceId(10L)).thenReturn(Optional.of(expired));

        linkService.syncNow("fid-1", null);

        verify(syncService, never()).syncLink(any());
    }
}
