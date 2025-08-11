package com.dongsoop.dongsoop.memberblock.repository;

import com.dongsoop.dongsoop.memberblock.dto.BlockedMember;
import java.util.List;

public interface MemberBlockRepositoryCustom {

    List<BlockedMember> findByBlockerId(Long blockerId);
}
