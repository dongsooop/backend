package com.dongsoop.dongsoop.report.service;

import com.dongsoop.dongsoop.chat.entity.ChatMessage;
import com.dongsoop.dongsoop.chat.entity.ChatMessageEntity;
import com.dongsoop.dongsoop.chat.entity.ChatRoom;
import com.dongsoop.dongsoop.chat.entity.MessageType;
import com.dongsoop.dongsoop.chat.exception.UnauthorizedChatAccessException;
import com.dongsoop.dongsoop.chat.repository.ChatMessageJpaRepository;
import com.dongsoop.dongsoop.chat.repository.RedisChatRepository;
import com.dongsoop.dongsoop.chat.service.ChatRoomService;
import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.member.exception.MemberNotFoundException;
import com.dongsoop.dongsoop.member.repository.MemberRepository;
import com.dongsoop.dongsoop.member.service.MemberService;
import com.dongsoop.dongsoop.report.dto.CreateChatReportRequest;
import com.dongsoop.dongsoop.report.entity.ChatMessageSnapshot;
import com.dongsoop.dongsoop.report.entity.ChatMessageSnapshots;
import com.dongsoop.dongsoop.report.entity.Report;
import com.dongsoop.dongsoop.report.entity.ReportType;
import com.dongsoop.dongsoop.report.exception.DuplicateReportException;
import com.dongsoop.dongsoop.report.exception.NotReportableMessageException;
import com.dongsoop.dongsoop.report.exception.ReportTargetNotFoundException;
import com.dongsoop.dongsoop.report.exception.SelfReportException;
import com.dongsoop.dongsoop.report.repository.ReportRepository;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatReportService {

    private static final int CONTEXT_SIZE = 10;
    private static final String DUPLICATE_REPORT_CONSTRAINT_NAME = "uk_report_reporter_message";

    private final MemberService memberService;
    private final MemberRepository memberRepository;
    private final ChatRoomService chatRoomService;
    private final RedisChatRepository redisChatRepository;
    private final ChatMessageJpaRepository chatMessageJpaRepository;
    private final ReportRepository reportRepository;

    @Transactional
    public void createReport(CreateChatReportRequest request) {
        Long reporterId = memberService.getMemberIdByAuthentication();
        validateParticipant(chatRoomService.getChatRoomById(request.roomId()), reporterId);

        List<ChatMessage> messages = loadMessages(request.roomId(), request.messageId());
        int targetIndex = indexOf(messages, request.messageId());
        ChatMessage target = messages.get(targetIndex);
        validateTarget(target, reporterId);

        if (reportRepository.existsByReporterIdAndMessageId(reporterId, request.messageId())) {
            throw new DuplicateReportException();
        }

        Member targetMember = memberRepository.findById(target.getSenderId())
                .orElseThrow(MemberNotFoundException::new);

        save(Report.builder()
                .reporter(memberRepository.getReferenceById(reporterId))
                .reportType(ReportType.CHAT_MESSAGE)
                .targetId(targetMember.getId())
                .targetMember(targetMember)
                .reportReason(request.reason())
                .description(request.description())
                .targetUrl("/chat/room/" + request.roomId())
                .chatRoomId(request.roomId())
                .messageId(target.getMessageId())
                .messageContent(ChatMessageSnapshot.truncate(target.getContent()))
                .messageSentAt(target.getTimestamp())
                .messageContext(buildContext(messages, targetIndex))
                .build());
    }

    private void validateParticipant(ChatRoom room, Long reporterId) {
        if (!room.getParticipants().contains(reporterId)) {
            throw new UnauthorizedChatAccessException();
        }
    }

    // ponytail: 방의 메시지를 전부 읽어 위치를 찾는다. 방당 30일치라 신고 빈도에서는 충분하고, 느려지면 ZSET 범위 조회로 바꾼다
    private List<ChatMessage> loadMessages(String roomId, String messageId) {
        List<ChatMessage> cached = redisChatRepository.findMessagesByRoomId(roomId);
        if (indexOf(cached, messageId) >= 0) {
            return sortByTime(cached);
        }

        List<ChatMessage> backedUp = chatMessageJpaRepository.findByRoomIdOrderByTimestampAsc(roomId).stream()
                .map(ChatMessageEntity::toChatMessage)
                .toList();
        if (indexOf(backedUp, messageId) < 0) {
            throw new ReportTargetNotFoundException(ReportType.CHAT_MESSAGE.name(), messageId);
        }

        return backedUp;
    }

    private static List<ChatMessage> sortByTime(List<ChatMessage> messages) {
        return messages.stream()
                .sorted(Comparator.comparing(ChatMessage::getTimestamp,
                        Comparator.nullsFirst(Comparator.<LocalDateTime>naturalOrder())))
                .toList();
    }

    private static int indexOf(List<ChatMessage> messages, String messageId) {
        for (int i = 0; i < messages.size(); i++) {
            if (Objects.equals(messages.get(i).getMessageId(), messageId)) {
                return i;
            }
        }
        return -1;
    }

    private static void validateTarget(ChatMessage target, Long reporterId) {
        if (!isUserMessage(target)) {
            throw new NotReportableMessageException();
        }

        if (Objects.equals(target.getSenderId(), reporterId)) {
            throw new SelfReportException();
        }
    }

    // 과거 메시지는 type이 비어 있을 수 있어 입장·퇴장만 시스템 메시지로 본다
    private static boolean isUserMessage(ChatMessage message) {
        return message.getType() != MessageType.ENTER && message.getType() != MessageType.LEAVE;
    }

    private static ChatMessageSnapshots buildContext(List<ChatMessage> messages, int targetIndex) {
        List<ChatMessageSnapshot> before = messages.subList(0, targetIndex).stream()
                .filter(ChatReportService::isUserMessage)
                .map(message -> ChatMessageSnapshot.of(
                        message.getSenderId(), message.getContent(), message.getTimestamp()))
                .toList();

        return new ChatMessageSnapshots(before.subList(Math.max(0, before.size() - CONTEXT_SIZE), before.size()));
    }

    private void save(Report report) {
        try {
            reportRepository.saveAndFlush(report);
        } catch (DataIntegrityViolationException e) {
            if (isDuplicateReportConstraintViolation(e)) {
                throw new DuplicateReportException();
            }
            throw e;
        }
    }

    private static boolean isDuplicateReportConstraintViolation(DataIntegrityViolationException e) {
        Throwable cause = e.getCause();
        while (cause != null) {
            if (cause instanceof ConstraintViolationException constraintViolation) {
                return DUPLICATE_REPORT_CONSTRAINT_NAME.equalsIgnoreCase(constraintViolation.getConstraintName());
            }
            cause = cause.getCause();
        }
        return false;
    }
}
