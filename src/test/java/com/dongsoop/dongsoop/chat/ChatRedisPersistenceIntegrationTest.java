package com.dongsoop.dongsoop.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dongsoop.dongsoop.chat.entity.ChatMessage;
import com.dongsoop.dongsoop.chat.entity.ChatRoom;
import com.dongsoop.dongsoop.chat.entity.MessageType;
import com.dongsoop.dongsoop.chat.repository.RedisChatRepository;
import com.dongsoop.dongsoop.common.config.RedisConfig;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
class ChatRedisPersistenceIntegrationTest {
  private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 14, 12, 0);

  @Container
  static final GenericContainer<?> redis =
      new GenericContainer<>("redis:7.4-alpine").withExposedPorts(6379);

  private static LettuceConnectionFactory connectionFactory;
  private static RedisTemplate<String, Object> template;
  private RedisChatRepository repository;

  @BeforeAll
  static void connect() {
    connectionFactory = new LettuceConnectionFactory(redis.getHost(), redis.getMappedPort(6379));
    connectionFactory.afterPropertiesSet();
    template = new RedisConfig().redisTemplate(connectionFactory);
    template.afterPropertiesSet();
  }

  @AfterAll
  static void disconnect() {
    if (connectionFactory != null) {
      connectionFactory.destroy();
    }
  }

  @BeforeEach
  void resetRedis() {
    try (RedisConnection connection = connectionFactory.getConnection()) {
      connection.serverCommands().flushDb();
    }
    repository = new RedisChatRepository(template);
  }

  @Test
  void roomRoundTripPreservesParticipantsTimesIndexesAndTtl() {
    ChatRoom room = ChatRoom.createWithParticipantsAndTitle(Set.of(8L), 7L, "모집 채팅");
    room.setCreatedAt(NOW.minusDays(1));
    room.setLastActivityAt(NOW);
    repository.saveRoom(room);
    ChatRoom loaded = repository.findRoomById(room.getRoomId()).orElseThrow();
    assertThat(loaded.getTitle()).isEqualTo("모집 채팅");
    assertThat(loaded.getParticipants()).containsExactlyInAnyOrder(7L, 8L);
    assertThat(loaded.getManagerId()).isEqualTo(7L);
    assertThat(loaded.getCreatedAt()).isEqualTo(NOW.minusDays(1));
    assertThat(loaded.getLastActivityAt()).isEqualTo(NOW);
    assertThat(repository.findRoomsByUserId(7L))
        .extracting(ChatRoom::getRoomId)
        .containsExactly(room.getRoomId());
    assertThat(repository.findRoomsByUserId(9L)).isEmpty();
    assertThat(template.getExpire("chat:room:" + room.getRoomId(), TimeUnit.SECONDS))
        .isBetween(TimeUnit.DAYS.toSeconds(29), TimeUnit.DAYS.toSeconds(30));
  }

  @Test
  void outOfOrderWritesHaveOrderedHistoryAndCursorBoundaries() {
    repository.saveMessage(message("m2", NOW));
    repository.saveMessage(message("m1", NOW.minusMinutes(1)));
    repository.saveMessage(message("m3", NOW.plusMinutes(1)));
    assertThat(repository.findMessagesByRoomId("room-7"))
        .extracting(ChatMessage::getMessageId)
        .containsExactly("m1", "m2", "m3");
    assertThat(repository.findMessagesByRoomIdAfterId("room-7", "m2"))
        .extracting(ChatMessage::getMessageId)
        .containsExactly("m3");
    assertThat(repository.findMessagesByRoomIdAfterTime("room-7", NOW))
        .extracting(ChatMessage::getMessageId)
        .containsExactly("m2", "m3");
    assertThat(repository.findLastMessageByRoomId("room-7").getMessageId()).isEqualTo("m3");
    assertThat(repository.findLastMessagesByRoomIds(List.of("room-7", "missing")))
        .containsOnlyKeys("room-7");
    assertThat(repository.findMessagesByRoomId("missing")).isEmpty();
    assertThatThrownBy(() -> repository.findMessagesByRoomIdAfterId("room-7", "unknown"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void directRoomLookupAndDeletionRemoveMessagesAndParticipantIndexes() {
    ChatRoom room =
        ChatRoom.builder()
            .roomId("direct-7")
            .participants(new java.util.HashSet<>(Set.of(7L, 8L)))
            .build();
    repository.saveRoom(room);
    ChatMessage message = message("m1", NOW);
    message.setRoomId(room.getRoomId());
    repository.saveMessage(message);
    assertThat(repository.findRoomByParticipants(8L, 7L)).isPresent();
    repository.deleteRoom(room.getRoomId());
    assertThat(repository.findRoomById(room.getRoomId())).isEmpty();
    assertThat(repository.findRoomsByUserId(7L)).isEmpty();
    assertThat(repository.findRoomsByUserId(8L)).isEmpty();
    assertThat(repository.findMessagesByRoomId(room.getRoomId())).isEmpty();
    assertThat(repository.findRoomByParticipants(7L, 8L)).isEmpty();
  }

  private ChatMessage message(String id, LocalDateTime time) {
    return ChatMessage.builder()
        .messageId(id)
        .roomId("room-7")
        .senderId(7L)
        .content("내용")
        .timestamp(time)
        .type(MessageType.CHAT)
        .build();
  }
}
