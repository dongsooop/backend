package com.dongsoop.dongsoop.report;

import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.member.repository.MemberRepository;
import com.dongsoop.dongsoop.member.service.MemberService;
import com.dongsoop.dongsoop.report.dto.SanctionStatusResponse;
import com.dongsoop.dongsoop.report.entity.Sanction;
import com.dongsoop.dongsoop.report.repository.SanctionRepository;
import com.dongsoop.dongsoop.report.service.ReportServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @InjectMocks
    private ReportServiceImpl reportService;

    @Mock
    private MemberService memberService;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private SanctionRepository sanctionRepository;

    @Test
    @DisplayName("제재 이력이 없는 사용자는 정상 상태를 반환한다")
    void checkAndUpdateSanctionStatus_WhenNoSanction_ShouldReturnNormalStatus() {
        // given
        Long memberId = 1L;
        when(memberService.getMemberIdByAuthentication()).thenReturn(memberId);
        when(sanctionRepository.findActiveSanctionByMemberId(memberId)).thenReturn(Optional.empty());

        // when
        SanctionStatusResponse response = reportService.checkAndUpdateSanctionStatus();

        // then
        assertThat(response.isSanctioned()).isFalse();
        assertThat(response.sanctionType()).isNull();
        assertThat(response.reason()).isNull();
        assertThat(response.startDate()).isNull();
        assertThat(response.endDate()).isNull();
        assertThat(response.description()).isNull();

        verify(sanctionRepository, never()).save(any());
    }

    @Test
    @DisplayName("활성 제재가 있는 사용자는 제재 정보를 반환한다")
    void checkAndUpdateSanctionStatus_WhenActiveSanction_ShouldReturnSanctionInfo() {
        // given
        Long memberId = 1L;
        Member member = Member.builder().id(memberId).build();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endDate = now.plusDays(10);

        Sanction sanction = Sanction.builder()
                .member(member)
                .sanctionType("TEMPORARY_BAN")
                .reason("부적절한 게시글")
                .startDate(now.minusDays(1))
                .endDate(endDate)
                .description("임시 정지")
                .isActive(true)
                .build();

        when(memberService.getMemberIdByAuthentication()).thenReturn(memberId);
        when(sanctionRepository.findActiveSanctionByMemberId(memberId)).thenReturn(Optional.of(sanction));

        // when
        SanctionStatusResponse response = reportService.checkAndUpdateSanctionStatus();

        // then
        assertThat(response.isSanctioned()).isTrue();
        assertThat(response.sanctionType()).isEqualTo("TEMPORARY_BAN");
        assertThat(response.reason()).isEqualTo("부적절한 게시글");
        assertThat(response.description()).isEqualTo("임시 정지");

        verify(sanctionRepository, never()).save(any());
    }

    @Test
    @DisplayName("만료된 제재는 자동으로 비활성화되고 정상 상태를 반환한다")
    void checkAndUpdateSanctionStatus_WhenExpiredSanction_ShouldDeactivateAndReturnNormal() {
        // given
        Long memberId = 1L;
        Member member = Member.builder().id(memberId).build();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiredEndDate = now.minusDays(1);

        Sanction expiredSanction = Sanction.builder()
                .member(member)
                .sanctionType("TEMPORARY_BAN")
                .reason("부적절한 게시글")
                .startDate(now.minusDays(10))
                .endDate(expiredEndDate)
                .description("임시 정지")
                .isActive(true)
                .build();

        when(memberService.getMemberIdByAuthentication()).thenReturn(memberId);
        when(sanctionRepository.findActiveSanctionByMemberId(memberId)).thenReturn(Optional.of(expiredSanction));

        // when
        SanctionStatusResponse response = reportService.checkAndUpdateSanctionStatus();

        // then
        assertThat(response.isSanctioned()).isFalse();
        assertThat(response.sanctionType()).isNull();

        verify(sanctionRepository).save(expiredSanction);
        assertThat(expiredSanction.getIsActive()).isFalse();
    }
}