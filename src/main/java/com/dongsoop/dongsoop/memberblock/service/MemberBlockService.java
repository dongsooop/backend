package com.dongsoop.dongsoop.memberblock.service;

import com.dongsoop.dongsoop.memberblock.dto.BlockedMember;
import com.dongsoop.dongsoop.memberblock.dto.MemberBlockRequest;
import java.util.List;

public interface MemberBlockService {

    void blockMember(MemberBlockRequest request);

    void unblockMember(MemberBlockRequest request);

    List<BlockedMember> getBlockedMember();
}
