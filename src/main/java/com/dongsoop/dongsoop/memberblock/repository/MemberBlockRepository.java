package com.dongsoop.dongsoop.memberblock.repository;

import com.dongsoop.dongsoop.memberblock.entity.MemberBlock;
import com.dongsoop.dongsoop.memberblock.entity.MemberBlockId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MemberBlockRepository extends JpaRepository<MemberBlock, MemberBlockId> {

    @Query("SELECT COUNT(mb) > 0 FROM MemberBlock mb WHERE mb.id.blocker.id = :blockerId AND mb.id.blockedMember.id = :blocked")
    boolean existsByBlockerIdAndBlockedId(@Param("blockerId") Long blockerId, @Param("blocked") Long blockedId);
}
