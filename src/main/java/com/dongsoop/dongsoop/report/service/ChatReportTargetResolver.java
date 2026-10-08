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
import java.util.Optional;
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
        if (targetIndex < 0) {
            throw new ReportTargetNotFoundException(ReportType.CHAT_MESSAGE.name(), request.messageId());
        }
        ChatMessage target = messages.get(targetIndex);
        validateTarget(target, reporterId);

        Member targetMember = memberRepository.findById(target.getSenderId())
                .orElseThrow(MemberNotFoundException::new);

        return new MessageReportDraft(ReportType.CHAT_MESSAGE, request.roomId(), "/chat/room/" + request.roomId(),
                targetMember, target.getMessageId(), ChatMessageSnapshot.truncate(target.getContent()),
                target.getTimestamp(), buildContext(messages, targetIndex));
    }

    /**
     * 신고된 메시지 다음의 사용자 메시지를 최대 10개 반환한다. 원문에서 신고된 메시지를 찾지 못하면 비어 있다.
     */
    public Optional<ChatMessageSnapshots> findContextAfter(String roomId, String messageId) {
        List<ChatMessage> messages = loadMessages(roomId, messageId);
        int targetIndex = indexOf(messages, messageId);
        if (targetIndex < 0) {
            return Optional.empty();
        }

        return Optional.of(new ChatMessageSnapshots(messages.subList(targetIndex + 1, messages.size()).stream()
                .filter(ChatReportTargetResolver::isUserMessage)
                .limit(CONTEXT_SIZE)
                .map(ChatReportTargetResolver::toSnapshot)
                .toList()));
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

        return chatMessageJpaRepository.findByRoomIdOrderByTimestampAsc(roomId).stream()
                .map(ChatMessageEntity::toChatMessage)
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

    private static ChatMessageSnapshot toSnapshot(ChatMessage message) {
        return ChatMessageSnapshot.of(message.getSenderId(), message.getContent(), message.getTimestamp());
    }

    private static ChatMessageSnapshots buildContext(List<ChatMessage> messages, int targetIndex) {
        List<ChatMessageSnapshot> before = messages.subList(0, targetIndex).stream()
                .filter(ChatReportTargetResolver::isUserMessage)
                .map(ChatReportTargetResolver::toSnapshot)
                .toList();

        return new ChatMessageSnapshots(before.subList(Math.max(0, before.size() - CONTEXT_SIZE), before.size()));
    }
}
