package com.dongsoop.dongsoop.memberblock.service;

import com.dongsoop.dongsoop.memberblock.dto.MemberBlockRequest;

public interface MemberBlockService {

    void blockMember(MemberBlockRequest request);

    void unblockMember(MemberBlockRequest request);
}
