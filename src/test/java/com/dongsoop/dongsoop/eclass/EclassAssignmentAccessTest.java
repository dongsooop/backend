package com.dongsoop.dongsoop.eclass;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dongsoop.dongsoop.common.exception.authentication.NotAuthenticationException;
import com.dongsoop.dongsoop.eclass.dto.EclassAssignmentListResponse;
import com.dongsoop.dongsoop.eclass.entity.EclassLink;
import com.dongsoop.dongsoop.eclass.repository.EclassAssignmentRepository;
import com.dongsoop.dongsoop.eclass.repository.EclassLinkRepository;
import com.dongsoop.dongsoop.eclass.service.EclassAssignmentServiceImpl;
import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.member.service.MemberService;
import com.dongsoop.dongsoop.memberdevice.entity.MemberDevice;
import com.dongsoop.dongsoop.eclass.service.EclassDeviceAccessor;
import com.dongsoop.dongsoop.memberdevice.service.NoticePreferenceDeviceResolver;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * 기기 식별자는 인증 수단이 아니므로, 남의 식별자로 과제를 열람할 수 없어야 한다.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EclassAssignmentAccessTest {

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

    private EclassAssignmentServiceImpl assignmentService;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW.toInstant(ZoneOffset.ofHours(9)), ZoneId.of("Asia/Seoul"));
        EclassDeviceAccessor deviceAccessor = new EclassDeviceAccessor(deviceResolver, memberService);
        assignmentService = new EclassAssignmentServiceImpl(deviceAccessor, linkRepository,
                assignmentRepository, clock);
        ReflectionTestUtils.setField(assignmentService, "baseUrl", "https://eclass.test");
    }

    private MemberDevice deviceOf(Member member) {
        return MemberDevice.builder()
                .id(10L)
                .deviceToken("fcm-token")
                .member(member)
                .build();
    }

    private void givenLinkedDevice(MemberDevice device) {
        when(deviceResolver.resolve(any(), any())).thenReturn(device);
        EclassLink link = new EclassLink(device, 1L, "테스트", "encrypted", NOW.minusDays(1));
        when(linkRepository.findByDeviceId(device.getId())).thenReturn(Optional.of(link));
    }

    @Test
    @DisplayName("회원 기기는 그 회원으로 로그인했을 때만 과제를 볼 수 있다")
    void ownerCanRead() {
        Member owner = Member.builder().id(OWNER_ID).build();
        givenLinkedDevice(deviceOf(owner));
        when(memberService.getMemberIdByAuthentication()).thenReturn(OWNER_ID);
        when(assignmentRepository.searchUpcomingByDevice(anyLong(), any(), anyInt())).thenReturn(List.of());

        EclassAssignmentListResponse response = assignmentService.getUpcoming("fid-1", null);

        assertThat(response.linked()).isTrue();
    }

    @Test
    @DisplayName("다른 회원의 기기 식별자로는 과제를 볼 수 없다")
    void otherMemberCannotRead() {
        Member owner = Member.builder().id(OWNER_ID).build();
        givenLinkedDevice(deviceOf(owner));
        when(memberService.getMemberIdByAuthentication()).thenReturn(OTHER_ID);

        EclassAssignmentListResponse response = assignmentService.getUpcoming("fid-1", null);

        assertThat(response.linked()).isFalse();
        verify(assignmentRepository, never()).searchUpcomingByDevice(anyLong(), any(), anyInt());
    }

    @Test
    @DisplayName("로그인하지 않은 요청은 회원 기기의 과제를 볼 수 없다")
    void anonymousCannotReadMemberDevice() {
        Member owner = Member.builder().id(OWNER_ID).build();
        givenLinkedDevice(deviceOf(owner));
        when(memberService.getMemberIdByAuthentication()).thenThrow(new NotAuthenticationException());

        EclassAssignmentListResponse response = assignmentService.getUpcoming("fid-1", null);

        assertThat(response.linked()).isFalse();
    }

    @Test
    @DisplayName("비회원 기기는 로그인 없이도 자기 과제를 볼 수 있다")
    void guestDeviceCanRead() {
        givenLinkedDevice(deviceOf(null));
        when(memberService.getMemberIdByAuthentication()).thenThrow(new NotAuthenticationException());
        when(assignmentRepository.searchUpcomingByDevice(anyLong(), any(), anyInt())).thenReturn(List.of());

        EclassAssignmentListResponse response = assignmentService.getUpcoming("fid-1", null);

        assertThat(response.linked()).isTrue();
    }

    @Test
    @DisplayName("홈 요약도 남의 기기 식별자로는 그 기기 과제를 쓰지 않는다")
    void homeSummaryIgnoresForeignDevice() {
        Member owner = Member.builder().id(OWNER_ID).build();
        givenLinkedDevice(deviceOf(owner));
        when(linkRepository.findAllByMemberIdAndStatus(any(), any())).thenReturn(List.of());

        assignmentService.getHomeSummary(OTHER_ID, "fid-1", null);

        verify(assignmentRepository, never()).countUpcomingByDevice(anyLong(), any());
    }

    @Test
    @DisplayName("비회원 홈 요약도 남의 기기 식별자로는 과제를 내주지 않는다")
    void anonymousHomeSummaryIgnoresMemberDevice() {
        Member owner = Member.builder().id(OWNER_ID).build();
        givenLinkedDevice(deviceOf(owner));
        when(memberService.getMemberIdByAuthentication()).thenThrow(new NotAuthenticationException());

        assertThat(assignmentService.getHomeSummary("fid-1", null).linked()).isFalse();
        verify(assignmentRepository, never()).countUpcomingByDevice(anyLong(), any());
    }
}
