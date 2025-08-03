package com.dongsoop.dongsoop.memberblock.service;

import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.member.repository.MemberRepository;
import com.dongsoop.dongsoop.memberblock.dto.MemberBlockRequest;
import com.dongsoop.dongsoop.memberblock.entity.MemberBlock;
import com.dongsoop.dongsoop.memberblock.entity.MemberBlockId;
import com.dongsoop.dongsoop.memberblock.exception.AlreadyBlockedByBlockerException;
import com.dongsoop.dongsoop.memberblock.exception.BlockNotFoundException;
import com.dongsoop.dongsoop.memberblock.repository.MemberBlockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MemberBlockServiceImpl implements MemberBlockService {

    private final MemberRepository memberRepository;

    private final MemberBlockRepository memberBlockRepository;

    @Override
    public void blockMember(MemberBlockRequest request) {
        Member blocker = memberRepository.getReferenceById(request.blockerId());
        Member blockedMemberId = memberRepository.getReferenceById(request.blockedMemberId());
        MemberBlockId memberBlockId = new MemberBlockId(blocker, blockedMemberId);

        if (memberBlockRepository.existsById(memberBlockId)) {
            throw new AlreadyBlockedByBlockerException();
        }

        MemberBlock memberBlock = new MemberBlock(memberBlockId);
        memberBlockRepository.save(memberBlock);
    }

    @Override
    public void unblockMember(MemberBlockRequest request) {
        Member blocker = memberRepository.getReferenceById(request.blockerId());
        Member blockedMemberId = memberRepository.getReferenceById(request.blockedMemberId());
        MemberBlockId memberBlockId = new MemberBlockId(blocker, blockedMemberId);

        if (!memberBlockRepository.existsById(memberBlockId)) {
            throw new BlockNotFoundException();
        }

        MemberBlock memberBlock = memberBlockRepository.getReferenceById(memberBlockId);
        memberBlockRepository.delete(memberBlock);
    }
}
