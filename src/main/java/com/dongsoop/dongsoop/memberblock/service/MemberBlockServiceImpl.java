package com.dongsoop.dongsoop.memberblock.service;

import com.dongsoop.dongsoop.chat.entity.BlockStatus;
import com.dongsoop.dongsoop.chat.entity.ChatRoom;
import com.dongsoop.dongsoop.chat.exception.ChatRoomNotFoundException;
import com.dongsoop.dongsoop.chat.repository.RedisChatRepository;
import com.dongsoop.dongsoop.chat.service.ChatService;
import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.member.repository.MemberRepository;
import com.dongsoop.dongsoop.member.service.MemberService;
import com.dongsoop.dongsoop.memberblock.dto.BlockedMember;
import com.dongsoop.dongsoop.memberblock.dto.MemberBlockRequest;
import com.dongsoop.dongsoop.memberblock.entity.MemberBlock;
import com.dongsoop.dongsoop.memberblock.entity.MemberBlockId;
import com.dongsoop.dongsoop.memberblock.exception.AlreadyBlockedByBlockerException;
import com.dongsoop.dongsoop.memberblock.exception.BlockNotFoundException;
import com.dongsoop.dongsoop.memberblock.repository.MemberBlockRepository;
import com.dongsoop.dongsoop.memberblock.repository.MemberBlockRepositoryCustom;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

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
        Member blocker = memberRepository.getReferenceById(request.blockerId());
        Member blockedMember = memberRepository.getReferenceById(request.blockedMemberId());
        MemberBlockId memberBlockId = new MemberBlockId(blocker, blockedMember);

        if (memberBlockRepository.existsById(memberBlockId)) {
            throw new AlreadyBlockedByBlockerException();
        }

        MemberBlock memberBlock = new MemberBlock(memberBlockId);
        memberBlockRepository.save(memberBlock);

        sendBlockWebsocketEvent(blocker, blockedMember, BlockStatus.I_BLOCKED);
    }


    @Override
    public void unblockMember(MemberBlockRequest request) {
        Member blocker = memberRepository.getReferenceById(request.blockerId());
        Member blockedMember = memberRepository.getReferenceById(request.blockedMemberId());
        MemberBlockId memberBlockId = new MemberBlockId(blocker, blockedMember);

        if (!memberBlockRepository.existsById(memberBlockId)) {
            throw new BlockNotFoundException();
        }

        MemberBlock memberBlock = memberBlockRepository.getReferenceById(memberBlockId);
        memberBlockRepository.delete(memberBlock);

        sendBlockWebsocketEvent(blocker, blockedMember, BlockStatus.NONE);
    }

    @Override
    public List<BlockedMember> getBlockedMember() {
        Long requesterId = memberService.getMemberIdByAuthentication();
        return memberBlockRepositoryCustom.findByBlockerId(requesterId);
    }

    private void sendBlockWebsocketEvent(Member blocker, Member blockedMember, BlockStatus blockStatus) {
        ChatRoom room = redisChatRepository.findRoomByParticipants(blocker.getId(), blockedMember.getId())
                .orElseThrow(ChatRoomNotFoundException::new);
        String roomId = room.getRoomId();

        chatService.sendBlockStatusToUser(roomId, blocker.getId(), blockStatus);
        chatService.sendBlockStatusToUser(roomId, blockedMember.getId(), BlockStatus.BLOCKED_BY_OTHER);
    }
}