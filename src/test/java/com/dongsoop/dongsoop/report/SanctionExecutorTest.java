package com.dongsoop.dongsoop.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dongsoop.dongsoop.chat.service.ChatParticipantService;
import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.member.repository.MemberRepository;
import com.dongsoop.dongsoop.report.entity.Report;
import com.dongsoop.dongsoop.report.entity.ReportType;
import com.dongsoop.dongsoop.report.entity.Sanction;
import com.dongsoop.dongsoop.report.entity.SanctionType;
import com.dongsoop.dongsoop.report.handler.ContentDeletionHandler;
import com.dongsoop.dongsoop.report.repository.ReportRepository;
import com.dongsoop.dongsoop.report.repository.SanctionRepository;
import com.dongsoop.dongsoop.report.service.SanctionExecutor;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class SanctionExecutorTest {

    @InjectMocks
    private SanctionExecutor sanctionExecutor;

    @Mock
    private ReportRepository reportRepository;
    @Mock
    private MemberRepository memberRepository;
    @Mock
    private ContentDeletionHandler contentDeletionHandler;
    @Mock
    private SanctionRepository sanctionRepository;
    @Mock
    private ChatParticipantService chatParticipantService;

    private final Member target = Member.builder().id(2L).build();
    private final Member systemAdmin = Member.builder().id(0L).build();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(sanctionExecutor, "systemAdminId", 0L);
    }

    @Test
    @DisplayName("경고 3회 누적 시 3일 정지를 시스템 관리자 명의와 필수 필드로 저장한다")
    void executeWarning_ThirdWarning_CreatesSuspensionWithRequiredFields() {
        Sanction warning = Sanction.builder().sanctionType(SanctionType.WARNING).build();
        Report report = Report.builder().id(1L).reportType(ReportType.MEMBER).targetId(2L)
                .targetMember(target).sanction(warning).build();
        when(reportRepository.countActiveWarningsForMember(2L, SanctionType.WARNING)).thenReturn(3L);
        when(memberRepository.getReferenceById(2L)).thenReturn(target);
        when(memberRepository.getReferenceById(0L)).thenReturn(systemAdmin);
        when(reportRepository.save(any(Report.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(sanctionRepository.save(any(Sanction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        sanctionExecutor.executeSanction(report);

        ArgumentCaptor<Sanction> captor = ArgumentCaptor.forClass(Sanction.class);
        verify(sanctionRepository).save(captor.capture());
        Sanction suspension = captor.getValue();
        assertThat(suspension.getSanctionType()).isEqualTo(SanctionType.TEMPORARY_BAN);
        assertThat(suspension.getAdmin()).isEqualTo(systemAdmin);
        assertThat(suspension.getMember()).isEqualTo(target);
        assertThat(suspension.getTargetMember()).isEqualTo(target);
        assertThat(suspension.getReport()).isNotNull();
        assertThat(suspension.getReport().getIsProcessed()).isTrue();
        assertThat(suspension.getReport().getSanction()).isEqualTo(suspension);
        assertThat(suspension.getEndDate()).isAfter(LocalDateTime.now().plusDays(2));
    }

    @Test
    @DisplayName("채팅방 추방 제재는 신고된 방에서 대상을 추방한다")
    void executeSanction_ChatKick_KicksFromReportedRoom() {
        Sanction kick = Sanction.builder().sanctionType(SanctionType.CHAT_KICK).build();
        Report report = Report.builder().id(1L).reportType(ReportType.CHAT_MESSAGE).targetId(2L)
                .targetMember(target).chatRoomId("room1").sanction(kick).build();

        sanctionExecutor.executeSanction(report);

        verify(chatParticipantService).kickUserByAdmin("room1", 2L);
    }
}
