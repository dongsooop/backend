package com.dongsoop.dongsoop.chat.repository;

import com.dongsoop.dongsoop.chat.entity.ChatRoomEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ChatRoomJpaRepository extends JpaRepository<ChatRoomEntity, String> {
    List<ChatRoomEntity> findByParticipantsContaining(String userId);
    Optional<ChatRoomEntity> findByParticipantsContainingAndIsGroupChat(String userId, boolean isGroupChat);
}