package com.dongsoop.dongsoop.chat;

import static org.assertj.core.api.Assertions.assertThat;

import com.dongsoop.dongsoop.AbstractIntegrationTest;
import com.dongsoop.dongsoop.chat.entity.ChatMessageEntity;
import com.dongsoop.dongsoop.chat.entity.ChatRoomEntity;
import com.dongsoop.dongsoop.chat.entity.MessageType;
import com.dongsoop.dongsoop.chat.repository.ChatMessageJpaRepository;
import com.dongsoop.dongsoop.chat.repository.ChatRoomJpaRepository;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(ChatDatabasePersistenceIntegrationTest.QueryConfig.class)
class ChatDatabasePersistenceIntegrationTest extends AbstractIntegrationTest {
  private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 14, 12, 0);
  @Autowired ChatRoomJpaRepository rooms;
  @Autowired ChatMessageJpaRepository messages;
  @Autowired EntityManager em;

  @TestConfiguration
  static class QueryConfig {
    @Bean
    JPAQueryFactory queryFactory(EntityManager em) {
      return new JPAQueryFactory(em);
    }
  }

  @Test
  void roomRoundTripKeepsParticipantSetAndRecoveryMetadata() {
    rooms.save(
        ChatRoomEntity.builder()
            .roomId("room-7")
            .title("모집 채팅")
            .managerId(7L)
            .isGroupChat(true)
            .participants(new HashSet<>(Set.of(7L, 8L)))
            .createdAt(NOW.minusDays(1))
            .lastActivityAt(NOW)
            .build());
    flushAndClear();
    ChatRoomEntity loaded = rooms.findById("room-7").orElseThrow();
    assertThat(loaded.getParticipants()).containsExactlyInAnyOrder(7L, 8L);
    assertThat(loaded.getTitle()).isEqualTo("모집 채팅");
    assertThat(loaded.getManagerId()).isEqualTo(7L);
    assertThat(loaded.isGroupChat()).isTrue();
    assertThat(loaded.getCreatedAt()).isEqualTo(NOW.minusDays(1));
    assertThat(loaded.getLastActivityAt()).isEqualTo(NOW);
    assertThat(rooms.findById("missing")).isEmpty();
  }

  @Test
  void messageLookupFiltersRoomAndSortsTimestampRegardlessOfWriteOrder() {
    messages.save(message("new", "room-7", NOW));
    messages.save(message("old", "room-7", NOW.minusMinutes(1)));
    messages.save(message("other", "room-8", NOW));
    flushAndClear();
    assertThat(messages.findByRoomIdOrderByTimestampAsc("room-7"))
        .extracting(ChatMessageEntity::getMessageId)
        .containsExactly("old", "new");
    assertThat(messages.findByRoomIdOrderByTimestampAsc("room-8"))
        .singleElement()
        .satisfies(
            message -> {
              assertThat(message.getSenderId()).isEqualTo(7L);
              assertThat(message.getContent()).isEqualTo("내용");
              assertThat(message.getType()).isEqualTo(MessageType.CHAT);
              assertThat(message.getTimestamp()).isEqualTo(NOW);
            });
    assertThat(messages.findByRoomIdOrderByTimestampAsc("missing")).isEmpty();
    messages.deleteById("old");
    flushAndClear();
    assertThat(messages.findByRoomIdOrderByTimestampAsc("room-7"))
        .extracting(ChatMessageEntity::getMessageId)
        .containsExactly("new");
  }

  private ChatMessageEntity message(String id, String room, LocalDateTime time) {
    return ChatMessageEntity.builder()
        .messageId(id)
        .roomId(room)
        .senderId(7L)
        .content("내용")
        .timestamp(time)
        .type(MessageType.CHAT)
        .build();
  }

  private void flushAndClear() {
    em.flush();
    em.clear();
  }
}
