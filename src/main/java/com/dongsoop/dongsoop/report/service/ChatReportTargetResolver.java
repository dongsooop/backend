package com.dongsoop.dongsoop.report.service;

import com.dongsoop.dongsoop.chat.entity.ChatMessage;
import com.dongsoop.dongsoop.chat.entity.ChatMessageEntity;
import com.dongsoop.dongsoop.chat.entity.ChatRoom;
import com.dongsoop.dongsoop.chat.entity.MessageType;
import com.dongsoop.dongsoop.chat.exception.UnauthorizedChatAccessException;
import com.dongsoop.dongsoop.chat.repository.ChatMessageJpaRepository;
import com.dongsoop.dongsoop.chat.service.ChatMessageService;
import com.dongsoop.dongsoop.chat.service.ChatRoomService;
import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.member.exception.MemberNotFoundException;
import com.dongsoop.dongsoop.member.repository.MemberRepository;
import com.dongsoop.dongsoop.report.dto.CreateChatReportRequest;
import com.dongsoop.dongsoop.report.dto.MessageReportDraft;
import com.dongsoop.dongsoop.report.entity.ChatMessageSnapshot;
import com.dongsoop.dongsoop.report.entity.ChatMessageSnapshots;
import com.dongsoop.dongsoop.report.entity.ReportType;
import com.dongsoop.dongsoop.report.exception.NotReportableMessageException;
import com.dongsoop.dongsoop.report.exception.ReportTargetNotFoundException;
import com.dongsoop.dongsoop.report.exception.SelfReportException;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChatReportTargetResolver {

    private static final int CONTEXT_SIZE = 10;

    private final MemberRepository memberRepository;
    private final ChatRoomService chatRoomService;
    private final ChatMessageService chatMessageService;
    private final ChatMessageJpaRepository chatMessageJpaRepository;

    public MessageReportDraft resolve(Long reporterId, CreateChatReportRequest request) {
        validateParticipant(chatRoomService.getChatRoomById(request.roomId()), reporterId);

        List<ChatMessage> messages = loadMessages(request.roomId(), request.messageId());
        int targetIndex = indexOf(messages, request.messageId());
        ChatMessage target = messages.get(targetIndex);
        validateTarget(target, reporterId);

        Member targetMember = memberRepository.findById(target.getSenderId())
                .orElseThrow(MemberNotFoundException::new);

        return new MessageReportDraft(ReportType.CHAT_MESSAGE, request.roomId(), "/chat/room/" + request.roomId(),
                targetMember, target.getMessageId(), ChatMessageSnapshot.truncate(target.getContent()),
                target.getTimestamp(), buildContext(messages, targetIndex));
    }

    private void validateParticipant(ChatRoom room, Long reporterId) {
        if (!room.getParticipants().contains(reporterId)) {
            throw new UnauthorizedChatAccessException();
        }
    }

    // ponytail: 방의 메시지를 전부 읽어 위치를 찾는다. 방당 30일치라 신고 빈도에서는 충분하고, 느려지면 ZSET 범위 조회로 바꾼다
    private List<ChatMessage> loadMessages(String roomId, String messageId) {
        List<ChatMessage> cached = chatMessageService.getAllMessages(roomId);
        if (indexOf(cached, messageId) >= 0) {
            return cached;
        }

        List<ChatMessage> backedUp = chatMessageJpaRepository.findByRoomIdOrderByTimestampAsc(roomId).stream()
                .map(ChatMessageEntity::toChatMessage)
                .toList();
        if (indexOf(backedUp, messageId) < 0) {
            throw new ReportTargetNotFoundException(ReportType.CHAT_MESSAGE.name(), messageId);
        }

        return backedUp;
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
                .filter(ChatReportTargetResolver::isUserMessage)
                .map(message -> ChatMessageSnapshot.of(
                        message.getSenderId(), message.getContent(), message.getTimestamp()))
                .toList();

        return new ChatMessageSnapshots(before.subList(Math.max(0, before.size() - CONTEXT_SIZE), before.size()));
    }
}
