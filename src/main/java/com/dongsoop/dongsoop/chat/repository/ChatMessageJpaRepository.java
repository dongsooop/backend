package com.dongsoop.dongsoop.chat.repository;

import com.dongsoop.dongsoop.chat.entity.ChatMessageEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageJpaRepository extends JpaRepository<ChatMessageEntity, String> {

  List<ChatMessageEntity> findByRoomIdOrderByTimestampAsc(String roomId);
}
