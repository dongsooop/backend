package com.dongsoop.dongsoop.memberblock.service;

import com.dongsoop.dongsoop.chat.entity.ChatRoom;
import com.dongsoop.dongsoop.chat.repository.RedisChatRepository;
import com.dongsoop.dongsoop.chat.service.ChatService;
import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.member.repository.MemberRepository;
import com.dongsoop.dongsoop.member.exception.MemberNotFoundException;
import com.dongsoop.dongsoop.member.service.MemberService;
import com.dongsoop.dongsoop.memberblock.dto.BlockedMember;
import com.dongsoop.dongsoop.memberblock.dto.MemberBlockRequest;
import com.dongsoop.dongsoop.memberblock.entity.MemberBlock;
import com.dongsoop.dongsoop.memberblock.entity.MemberBlockId;
import com.dongsoop.dongsoop.memberblock.exception.AlreadyBlockedByBlockerException;
import com.dongsoop.dongsoop.memberblock.exception.BlockNotFoundException;
import com.dongsoop.dongsoop.memberblock.exception.SelfBlockException;
import com.dongsoop.dongsoop.memberblock.repository.MemberBlockRepository;
import com.dongsoop.dongsoop.memberblock.repository.MemberBlockRepositoryCustom;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberBlockServiceImpl implements MemberBlockService {

    private final MemberService memberService;
    private final MemberRepository memberRepository;
    private final MemberBlockRepository memberBlockRepository;
    private final MemberBlockRepositoryCustom memberBlockRepositoryCustom;
    private final ChatService chatService;
    private final RedisChatRepository redisChatRepository;

    @Override
    public void blockMember(MemberBlockRequest request) {
        Long blockerId = memberService.getMemberIdByAuthentication();
        validateBlockTarget(blockerId, request.blockedMemberId());

        Member blocker = memberRepository.getReferenceById(blockerId);
        Member blockedMember = memberRepository.getReferenceById(request.blockedMemberId());
        MemberBlockId memberBlockId = new MemberBlockId(blocker, blockedMember);

        if (memberBlockRepository.existsById(memberBlockId)) {
            throw new AlreadyBlockedByBlockerException();
        }

        // 메서드 트랜잭션이 없어 save가 자체 트랜잭션에서 커밋하므로 연타 경합의 키 중복은 여기서 드러난다
        try {
            memberBlockRepository.save(new MemberBlock(memberBlockId));
        } catch (DataIntegrityViolationException e) {
            throw new AlreadyBlockedByBlockerException();
        }

        sendBlockWebsocketEvent(blocker, blockedMember);
    }

    @Override
    public void unblockMember(MemberBlockRequest request) {
        Long blockerId = memberService.getMemberIdByAuthentication();

        if (request.blockedMemberId() == null) {
            throw new BlockNotFoundException();
        }

        Member blocker = memberRepository.getReferenceById(blockerId);
        Member blockedMember = memberRepository.getReferenceById(request.blockedMemberId());
        MemberBlockId memberBlockId = new MemberBlockId(blocker, blockedMember);

        if (!memberBlockRepository.existsById(memberBlockId)) {
            throw new BlockNotFoundException();
        }

        MemberBlock memberBlock = memberBlockRepository.getReferenceById(memberBlockId);
        memberBlockRepository.delete(memberBlock);

        sendBlockWebsocketEvent(blocker, blockedMember);
    }

    @Override
    public List<BlockedMember> getBlockedMember() {
        Long requesterId = memberService.getMemberIdByAuthentication();
        return memberBlockRepositoryCustom.findByBlockerId(requesterId);
    }

    private void validateBlockTarget(Long blockerId, Long blockedMemberId) {
        if (blockerId.equals(blockedMemberId)) {
            throw new SelfBlockException();
        }

        if (blockedMemberId == null || !memberRepository.existsById(blockedMemberId)) {
            throw new MemberNotFoundException();
        }
    }

    // 서로 차단한 경우가 있어 고정값 대신 저장된 차단 관계로 상태를 다시 계산해 보낸다
    private void sendBlockWebsocketEvent(Member blocker, Member blockedMember) {
        ChatRoom room = redisChatRepository.findRoomByParticipants(blocker.getId(), blockedMember.getId())
                .orElse(null);

        if (room == null) {
            log.info("채팅방이 존재하지 않습니다. 차단만 처리됩니다. Blocker: {}, Blocked: {}",
                    blocker.getId(), blockedMember.getId());
            return;
        }

        String roomId = room.getRoomId();
        chatService.sendBlockStatusToUser(roomId, blocker.getId(), chatService.getBlockStatus(roomId, blocker.getId()));
        chatService.sendBlockStatusToUser(roomId, blockedMember.getId(),
                chatService.getBlockStatus(roomId, blockedMember.getId()));
    }
}