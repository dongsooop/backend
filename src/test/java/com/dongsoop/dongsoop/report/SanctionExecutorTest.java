package com.dongsoop.dongsoop.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dongsoop.dongsoop.blinddate.service.BlindDateService;
import com.dongsoop.dongsoop.chat.service.ChatParticipantService;
import com.dongsoop.dongsoop.jwt.service.DeviceBlacklistService;
import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.member.repository.MemberRepository;
import com.dongsoop.dongsoop.memberdevice.entity.MemberDevice;
import com.dongsoop.dongsoop.memberdevice.entity.MemberDeviceType;
import com.dongsoop.dongsoop.memberdevice.repository.MemberDeviceRepository;
import com.dongsoop.dongsoop.report.entity.Report;
import com.dongsoop.dongsoop.report.entity.ReportReason;
import com.dongsoop.dongsoop.report.entity.ReportType;
import com.dongsoop.dongsoop.report.entity.Sanction;
import com.dongsoop.dongsoop.report.entity.SanctionType;
import com.dongsoop.dongsoop.report.exception.ReportAlreadyProcessedException;
import com.dongsoop.dongsoop.report.exception.SanctionEndDateRequiredException;
import com.dongsoop.dongsoop.report.handler.ContentDeletionHandler;
import com.dongsoop.dongsoop.report.repository.ReportRepository;
import com.dongsoop.dongsoop.report.repository.SanctionRepository;
import com.dongsoop.dongsoop.report.service.SanctionExecutor;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@ExtendWith(MockitoExtension.class)
class SanctionExecutorTest {

  @InjectMocks private SanctionExecutor sanctionExecutor;

  @Mock private ReportRepository reportRepository;
  @Mock private MemberRepository memberRepository;
  @Mock private ContentDeletionHandler contentDeletionHandler;
  @Mock private SanctionRepository sanctionRepository;
  @Mock private ChatParticipantService chatParticipantService;
  @Mock private MemberDeviceRepository memberDeviceRepository;
  @Mock private DeviceBlacklistService deviceBlacklistService;
  @Mock private BlindDateService blindDateService;

  private final Member admin = Member.builder().id(100L).build();
  private final Member target = Member.builder().id(2L).build();
  private final Member systemAdmin = Member.builder().id(0L).build();

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(sanctionExecutor, "systemAdminId", 0L);
  }

  private static Report memberReport() {
    return Report.builder().id(1L).reportType(ReportType.MEMBER).targetId(2L).build();
  }

  private Sanction issue(SanctionType type, String reason, LocalDateTime requestedEndAt) {
    return sanctionExecutor.issue(
        memberReport(), admin, target, type, reason, requestedEndAt, null);
  }

  @Test
  @DisplayName("제재는 관리자·대상·신고·사유·설명을 채워 저장하고 신고를 처리 완료한다")
  void issue_FillsRequiredFieldsAndProcessesReport() {
    Report report = memberReport();

    Sanction sanction =
        sanctionExecutor.issue(report, admin, target, SanctionType.WARNING, null, null, null);

    verify(sanctionRepository).saveAndFlush(sanction);
    assertThat(sanction.getAdmin()).isEqualTo(admin);
    assertThat(sanction.getMember()).isEqualTo(target);
    assertThat(sanction.getTargetMember()).isEqualTo(target);
    assertThat(sanction.getReport()).isEqualTo(report);
    assertThat(sanction.getSanctionType()).isEqualTo(SanctionType.WARNING);
    assertThat(sanction.getReason()).isEqualTo("경고");
    assertThat(sanction.getDescription()).isEqualTo("경고");
    assertThat(sanction.getStartDate()).isNotNull();
    assertThat(report.getIsProcessed()).isTrue();
    assertThat(report.getSanction()).isEqualTo(sanction);
    assertThat(report.getAdmin()).isEqualTo(admin);
    assertThat(report.getTargetMember()).isEqualTo(target);
  }

  @Test
  @DisplayName("입력한 사유와 설명은 그대로 저장한다")
  void issue_KeepsGivenReasonAndDescription() {
    Sanction sanction =
        sanctionExecutor.issue(
            memberReport(), admin, target, SanctionType.PERMANENT_BAN, "욕설", null, "설명");

    assertThat(sanction.getReason()).isEqualTo("욕설");
    assertThat(sanction.getDescription()).isEqualTo("설명");
  }

  @Test
  @DisplayName("경고와 영구정지는 종료일이 없으면 영구 종료일로 저장한다")
  void issue_WarningAndPermanentBanWithoutEndDate_UsePermanentEndDate() {
    assertThat(issue(SanctionType.WARNING, "욕설", null).getEndDate())
        .isEqualTo(SanctionType.PERMANENT_END_DATE);
    assertThat(issue(SanctionType.PERMANENT_BAN, "욕설", null).getEndDate())
        .isEqualTo(SanctionType.PERMANENT_END_DATE);
  }

  @Test
  @DisplayName("일시정지는 종료일이 없으면 거절하고 저장하지 않는다")
  void issue_TemporaryBanWithoutEndDate_Throws() {
    assertThatThrownBy(() -> issue(SanctionType.TEMPORARY_BAN, "욕설", null))
        .isInstanceOf(SanctionEndDateRequiredException.class);
    verify(sanctionRepository, never()).saveAndFlush(any());
  }

  @Test
  @DisplayName("일시정지는 입력한 종료일로 저장한다")
  void issue_TemporaryBanWithEndDate_UsesRequestedEndDate() {
    LocalDateTime endAt = LocalDateTime.of(2026, 10, 5, 0, 0);

    assertThat(issue(SanctionType.TEMPORARY_BAN, "욕설", endAt).getEndDate()).isEqualTo(endAt);
  }

  @Test
  @DisplayName("게시글 삭제는 기간 개념이 없어 시작 시각을 종료일로 쓰고 게시글을 지운다")
  void issue_ContentDeletion_EndsAtStartAndDeletesContent() {
    Report report =
        Report.builder().id(1L).reportType(ReportType.PROJECT_BOARD).targetId(10L).build();

    Sanction sanction =
        sanctionExecutor.issue(
            report, admin, target, SanctionType.CONTENT_DELETION, null, null, null);

    assertThat(sanction.getEndDate()).isEqualTo(sanction.getStartDate());
    verify(contentDeletionHandler).deleteContent(report);
  }

  @Test
  @DisplayName("이미 처리된 신고에는 제재를 실행하지 않는다")
  void issue_AlreadyProcessed_Throws() {
    Report processed =
        Report.builder()
            .id(1L)
            .reportType(ReportType.CHAT_MESSAGE)
            .targetId(2L)
            .chatRoomId("room1")
            .isProcessed(true)
            .build();

    assertThatThrownBy(
            () ->
                sanctionExecutor.issue(
                    processed, admin, target, SanctionType.CHAT_KICK, null, null, null))
        .isInstanceOf(ReportAlreadyProcessedException.class)
        .hasMessageContaining("이미 처리된 신고입니다");
    verify(sanctionRepository, never()).saveAndFlush(any());
    verify(chatParticipantService, never()).kickUserByAdmin(any(), anyLong());
  }

  @Test
  @DisplayName("채팅방 추방은 제재를 flush한 뒤 신고된 방에서 대상을 추방한다")
  void issue_ChatKick_FlushesBeforeKick() {
    Report report =
        Report.builder()
            .id(1L)
            .reportType(ReportType.CHAT_MESSAGE)
            .targetId(2L)
            .targetMember(target)
            .chatRoomId("room1")
            .build();

    Sanction sanction =
        sanctionExecutor.issue(report, admin, target, SanctionType.CHAT_KICK, null, null, null);

    assertThat(sanction.getEndDate()).isEqualTo(sanction.getStartDate());
    InOrder inOrder = inOrder(sanctionRepository, chatParticipantService);
    inOrder.verify(sanctionRepository).saveAndFlush(sanction);
    inOrder.verify(chatParticipantService).kickUserByAdmin("room1", 2L);
  }

  @Test
  @DisplayName("과팅 신고의 채팅방 추방은 커밋된 뒤 신고된 세션에서 대상을 강퇴하고 채팅방 추방은 하지 않는다")
  void issue_BlindDateChatKick_KicksParticipantOnlyAfterCommit() {
    Report report =
        Report.builder()
            .id(1L)
            .reportType(ReportType.BLINDDATE_MESSAGE)
            .targetId(2L)
            .targetMember(target)
            .chatRoomId("session-1")
            .build();
    TransactionSynchronizationManager.initSynchronization();
    try {
      sanctionExecutor.issue(report, admin, target, SanctionType.CHAT_KICK, null, null, null);
      verify(blindDateService, never()).kickParticipant(any(), anyLong());

      TransactionSynchronizationManager.getSynchronizations()
          .forEach(TransactionSynchronization::afterCommit);
    } finally {
      TransactionSynchronizationManager.clearSynchronization();
    }

    verify(blindDateService).kickParticipant("session-1", 2L);
    verify(chatParticipantService, never()).kickUserByAdmin(any(), anyLong());
  }

  @Test
  @DisplayName("과팅 강퇴 제재가 롤백되면 강퇴하지 않는다")
  void issue_BlindDateChatKickRolledBack_DoesNotKick() {
    Report report =
        Report.builder()
            .id(1L)
            .reportType(ReportType.BLINDDATE_MESSAGE)
            .targetId(2L)
            .targetMember(target)
            .chatRoomId("session-1")
            .build();
    TransactionSynchronizationManager.initSynchronization();
    try {
      sanctionExecutor.issue(report, admin, target, SanctionType.CHAT_KICK, null, null, null);

      TransactionSynchronizationManager.getSynchronizations()
          .forEach(sync -> sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
    } finally {
      TransactionSynchronizationManager.clearSynchronization();
    }

    verify(blindDateService, never()).kickParticipant(any(), anyLong());
  }

  @Test
  @DisplayName("경고 3회 누적 시 3일 정지를 시스템 관리자 명의와 필수 필드로 저장한다")
  void issue_ThirdWarning_CreatesSuspensionWithRequiredFields() {
    when(reportRepository.countActiveWarningsForMember(2L, SanctionType.WARNING)).thenReturn(3L);
    when(memberRepository.getReferenceById(0L)).thenReturn(systemAdmin);
    when(reportRepository.save(any(Report.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    issue(SanctionType.WARNING, "욕설", null);

    ArgumentCaptor<Sanction> captor = ArgumentCaptor.forClass(Sanction.class);
    verify(sanctionRepository, times(2)).saveAndFlush(captor.capture());
    List<Sanction> saved = captor.getAllValues();
    assertThat(saved.get(0).getSanctionType()).isEqualTo(SanctionType.WARNING);
    Sanction suspension = saved.get(1);
    assertThat(suspension.getSanctionType()).isEqualTo(SanctionType.TEMPORARY_BAN);
    assertThat(suspension.getAdmin()).isEqualTo(systemAdmin);
    assertThat(suspension.getMember()).isEqualTo(target);
    assertThat(suspension.getTargetMember()).isEqualTo(target);
    assertThat(suspension.getReason()).isEqualTo("욕설");
    assertThat(suspension.getDescription()).isEqualTo("경고 3회 누적으로 인한 자동 3일 정지");
    assertThat(suspension.getReport().getReportType()).isEqualTo(ReportType.MEMBER);
    assertThat(suspension.getReport().getDescription()).isEqualTo("경고 3회 누적으로 인한 자동 3일 정지");
    assertThat(suspension.getReport().getIsProcessed()).isTrue();
    assertThat(suspension.getReport().getSanction()).isEqualTo(suspension);
    assertThat(suspension.getEndDate()).isAfter(LocalDateTime.now().plusDays(2));
  }

  @Test
  @DisplayName("경고가 누적 기준에 닿지 않으면 정지를 만들지 않는다")
  void issue_SecondWarning_DoesNotSuspend() {
    when(reportRepository.countActiveWarningsForMember(2L, SanctionType.WARNING)).thenReturn(2L);

    issue(SanctionType.WARNING, "욕설", null);

    verify(sanctionRepository, times(1)).saveAndFlush(any());
    verify(reportRepository, never()).save(any());
  }

  @Test
  @DisplayName("시스템 제재는 시스템 관리자 명의로 신고된 회원에게 준다")
  void issueBySystem_UsesSystemAdminAndReportedMember() {
    Report report =
        Report.builder()
            .id(1L)
            .reportType(ReportType.PROJECT_BOARD)
            .targetId(10L)
            .targetMember(target)
            .build();
    when(memberRepository.getReferenceById(0L)).thenReturn(systemAdmin);

    Sanction sanction =
        sanctionExecutor.issueBySystem(
            report, SanctionType.CONTENT_DELETION, "부적절한 언어 사용", "자동 제재에 의한 게시글 삭제");

    assertThat(sanction.getAdmin()).isEqualTo(systemAdmin);
    assertThat(sanction.getTargetMember()).isEqualTo(target);
    assertThat(sanction.getReason()).isEqualTo("부적절한 언어 사용");
    assertThat(sanction.getDescription()).isEqualTo("자동 제재에 의한 게시글 삭제");
    assertThat(report.getIsProcessed()).isTrue();
    verify(contentDeletionHandler).deleteContent(report);
  }

  private static MemberDevice device(long id) {
    return MemberDevice.builder().id(id).memberDeviceType(MemberDeviceType.ANDROID).build();
  }

  @Test
  @DisplayName("자동 정지 사유는 마지막 경고의 사유를, 내부 회원 신고 사유는 그 경고 신고의 사유를 쓴다")
  void issue_FifthWarning_SuspensionUsesLastWarningReason() {
    when(reportRepository.countActiveWarningsForMember(2L, SanctionType.WARNING)).thenReturn(5L);
    when(memberRepository.getReferenceById(0L)).thenReturn(systemAdmin);
    when(reportRepository.save(any(Report.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    Report warned =
        Report.builder()
            .id(1L)
            .reportType(ReportType.CHAT_MESSAGE)
            .targetId(2L)
            .reportReason(ReportReason.HATE_SPEECH)
            .build();

    sanctionExecutor.issue(warned, admin, target, SanctionType.WARNING, "혐오 표현", null, null);

    ArgumentCaptor<Sanction> captor = ArgumentCaptor.forClass(Sanction.class);
    verify(sanctionRepository, times(2)).saveAndFlush(captor.capture());
    Sanction suspension = captor.getAllValues().get(1);
    assertThat(suspension.getReason()).isEqualTo("혐오 표현");
    assertThat(suspension.getDescription()).isEqualTo("경고 5회 누적으로 인한 자동 14일 정지");
    assertThat(suspension.getReport().getReportReason()).isEqualTo(ReportReason.HATE_SPEECH);
  }

  @Test
  @DisplayName("마지막 경고 사유가 비어 있으면 자동 정지 사유는 기존 누적 정지 문구로 대체한다")
  void issue_ThirdWarningWithBlankReason_FallsBackToDescription() {
    when(reportRepository.countActiveWarningsForMember(2L, SanctionType.WARNING)).thenReturn(3L);
    when(memberRepository.getReferenceById(0L)).thenReturn(systemAdmin);
    when(reportRepository.save(any(Report.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    issue(SanctionType.WARNING, " ", null);

    ArgumentCaptor<Sanction> captor = ArgumentCaptor.forClass(Sanction.class);
    verify(sanctionRepository, times(2)).saveAndFlush(captor.capture());
    Sanction suspension = captor.getAllValues().get(1);
    assertThat(suspension.getReason()).isEqualTo("경고 3회 누적으로 인한 자동 3일 정지");
    assertThat(suspension.getReport().getReportReason()).isEqualTo(ReportReason.OTHER);
  }

  @Test
  @DisplayName("관리자가 사유 없이 발급해 기본값(경고)이 채워진 경고면 자동 정지 사유는 누적 정지 문구로 대체한다")
  void issue_ThirdWarningWithDefaultReason_FallsBackToDescription() {
    when(reportRepository.countActiveWarningsForMember(2L, SanctionType.WARNING)).thenReturn(3L);
    when(memberRepository.getReferenceById(0L)).thenReturn(systemAdmin);
    when(reportRepository.save(any(Report.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    issue(SanctionType.WARNING, null, null);

    ArgumentCaptor<Sanction> captor = ArgumentCaptor.forClass(Sanction.class);
    verify(sanctionRepository, times(2)).saveAndFlush(captor.capture());
    assertThat(captor.getAllValues().get(0).getReason()).isEqualTo("경고");
    assertThat(captor.getAllValues().get(1).getReason()).isEqualTo("경고 3회 누적으로 인한 자동 3일 정지");
  }

  @Test
  @DisplayName("정지는 커밋된 뒤에만 회원의 모든 디바이스를 블랙리스트에 올린다")
  void issue_Ban_BlacklistsAllDevicesOnlyAfterCommit() {
    when(memberDeviceRepository.findByMemberId(2L)).thenReturn(List.of(device(10L), device(11L)));
    TransactionSynchronizationManager.initSynchronization();
    try {
      issue(SanctionType.PERMANENT_BAN, "욕설", null);
      verify(deviceBlacklistService, never()).blacklist(anyLong());

      TransactionSynchronizationManager.getSynchronizations()
          .forEach(TransactionSynchronization::afterCommit);
    } finally {
      TransactionSynchronizationManager.clearSynchronization();
    }

    verify(deviceBlacklistService).blacklist(10L);
    verify(deviceBlacklistService).blacklist(11L);
  }

  @Test
  @DisplayName("정지 트랜잭션이 롤백되면 디바이스를 블랙리스트에 올리지 않는다")
  void issue_BanRolledBack_DoesNotBlacklist() {
    when(memberDeviceRepository.findByMemberId(2L)).thenReturn(List.of(device(10L)));
    TransactionSynchronizationManager.initSynchronization();
    try {
      issue(SanctionType.TEMPORARY_BAN, "욕설", LocalDateTime.now().plusDays(1));

      TransactionSynchronizationManager.getSynchronizations()
          .forEach(sync -> sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
    } finally {
      TransactionSynchronizationManager.clearSynchronization();
    }

    verify(deviceBlacklistService, never()).blacklist(anyLong());
  }

  @Test
  @DisplayName("블랙리스트 반영이 실패해도 정지는 유지된다")
  void issue_BlacklistFails_KeepsBan() {
    when(memberDeviceRepository.findByMemberId(2L)).thenReturn(List.of(device(10L)));
    doThrow(new IllegalStateException("redis down")).when(deviceBlacklistService).blacklist(10L);
    Report report = memberReport();

    Sanction sanction =
        sanctionExecutor.issue(report, admin, target, SanctionType.PERMANENT_BAN, "욕설", null, null);

    assertThat(report.getSanction()).isEqualTo(sanction);
    assertThat(report.getIsProcessed()).isTrue();
  }

  @Test
  @DisplayName("경고 3회 누적 자동 정지도 디바이스를 블랙리스트에 올리고, 정지가 아닌 경고는 건드리지 않는다")
  void issue_AutoSuspensionBlacklists_WarningAloneDoesNot() {
    when(reportRepository.countActiveWarningsForMember(2L, SanctionType.WARNING))
        .thenReturn(2L, 3L);
    when(memberRepository.getReferenceById(0L)).thenReturn(systemAdmin);
    when(reportRepository.save(any(Report.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(memberDeviceRepository.findByMemberId(2L)).thenReturn(List.of(device(10L)));

    issue(SanctionType.WARNING, "욕설", null);
    verify(deviceBlacklistService, never()).blacklist(anyLong());

    issue(SanctionType.WARNING, "욕설", null);
    verify(deviceBlacklistService).blacklist(10L);
  }
}
