package com.dongsoop.dongsoop.blinddate;

import com.dongsoop.dongsoop.blinddate.gateway.BlindDateGateway;
import com.dongsoop.dongsoop.blinddate.handler.BlindDateChoiceHandler;
import com.dongsoop.dongsoop.blinddate.handler.BlindDateConnectHandler;
import com.dongsoop.dongsoop.blinddate.handler.BlindDateDisconnectHandler;
import com.dongsoop.dongsoop.blinddate.handler.BlindDateMessageHandler;
import com.dongsoop.dongsoop.blinddate.lock.BlindDateMatchingLock;
import com.dongsoop.dongsoop.blinddate.lock.BlindDateMemberLock;
import com.dongsoop.dongsoop.blinddate.lock.BlindDateSessionLock;
import com.dongsoop.dongsoop.blinddate.notification.BlindDateNotification;
import com.dongsoop.dongsoop.blinddate.repository.BlindDateParticipantStorage;
import com.dongsoop.dongsoop.blinddate.repository.BlindDateParticipantStorageImpl;
import com.dongsoop.dongsoop.blinddate.repository.BlindDateSessionStorage;
import com.dongsoop.dongsoop.blinddate.repository.BlindDateSessionStorageImpl;
import com.dongsoop.dongsoop.blinddate.repository.BlindDateStorage;
import com.dongsoop.dongsoop.blinddate.repository.BlindDateStorageImpl;
import com.dongsoop.dongsoop.blinddate.scheduler.BlindDateSessionScheduler;
import com.dongsoop.dongsoop.blinddate.scheduler.BlindDateTaskScheduler;
import com.dongsoop.dongsoop.blinddate.service.BlindDateService;
import com.dongsoop.dongsoop.blinddate.service.BlindDateServiceImpl;
import com.dongsoop.dongsoop.blinddate.service.BlindDateSessionService;
import com.dongsoop.dongsoop.blinddate.service.BlindDateSessionServiceImpl;
import com.dongsoop.dongsoop.chat.service.ChatRoomService;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Primary;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket 통합 테스트를 위한 최소 설정
 */
@TestConfiguration
@EnableWebSocketMessageBroker
@ComponentScan(basePackages = {"com.dongsoop.dongsoop.jwt"})
public class WebSocketTestConfig implements WebSocketMessageBrokerConfigurer {

    @Bean
    @Primary
    public BlindDateStorageImpl blindDateStorage() {
        return new BlindDateStorageImpl();
    }

    @Bean
    @Primary
    public BlindDateParticipantStorage participantStorage() {
        return new BlindDateParticipantStorageImpl();
    }

    @Bean
    @Primary
    public BlindDateSessionStorage sessionStorage() {
        return new BlindDateSessionStorageImpl();
    }

    @Bean
    @Primary
    public BlindDateMatchingLock blindDateMatchingLock() {
        return new BlindDateMatchingLock();
    }

    @Bean
    @Primary
    public BlindDateMemberLock blindDateMemberLock() {
        return new BlindDateMemberLock();
    }

    @Bean
    @Primary
    public BlindDateTaskScheduler blindDateTaskScheduler() {
        BlindDateTaskScheduler scheduler = new BlindDateTaskScheduler();
        return scheduler;
    }

    @Bean
    @Primary
    public BlindDateSessionScheduler blindDateSessionScheduler() {
        return Mockito.mock(BlindDateSessionScheduler.class);
    }

    @Bean
    @Primary
    public BlindDateNotification blindDateNotification() {
        return Mockito.mock(BlindDateNotification.class);
    }

    @Bean
    @Primary
    public ChatRoomService chatRoomService() {
        return Mockito.mock(ChatRoomService.class);
    }

    @Bean
    @Primary
    public BlindDateService blindDateService(
            BlindDateParticipantStorage participantStorage,
            BlindDateStorage blindDateStorage,
            BlindDateNotification notification,
            BlindDateSessionStorage sessionStorage,
            SimpMessagingTemplate messagingTemplate,
            BlindDateTaskScheduler taskScheduler
    ) {
        return new BlindDateServiceImpl(
                participantStorage,
                blindDateStorage,
                notification,
                sessionStorage,
                messagingTemplate,
                taskScheduler
        );
    }

    @Bean
    @Primary
    public BlindDateSessionService blindDateSessionService(
            BlindDateParticipantStorage participantStorage,
            BlindDateStorageImpl blindDateStorage
    ) {
        return new BlindDateSessionServiceImpl(
                participantStorage,
                blindDateStorage
        );
    }

    @Bean
    @Primary
    public BlindDateConnectHandler blindDateConnectHandler(
            BlindDateParticipantStorage participantStorage,
            BlindDateStorageImpl blindDateStorage,
            BlindDateSessionStorage sessionStorage,
            BlindDateService blindDateService,
            BlindDateSessionService sessionService,
            BlindDateSessionScheduler sessionScheduler,
            SimpMessagingTemplate messagingTemplate,
            BlindDateMatchingLock matchingLock,
            BlindDateMemberLock memberLock,
            BlindDateSessionLock sessionLock
    ) {
        return new BlindDateConnectHandler(
                participantStorage,
                blindDateStorage,
                sessionStorage,
                blindDateService,
                sessionService,
                sessionScheduler,
                messagingTemplate,
                matchingLock,
                memberLock,
                sessionLock
        );
    }

    @Bean
    @Primary
    public BlindDateDisconnectHandler blindDateDisconnectHandler(
            BlindDateParticipantStorage participantStorage,
            BlindDateSessionStorage sessionStorage,
            BlindDateService blindDateService,
            BlindDateMatchingLock matchingLock,
            BlindDateMemberLock memberLock,
            BlindDateSessionLock sessionLock
    ) {
        return new BlindDateDisconnectHandler(
                participantStorage,
                sessionStorage,
                blindDateService,
                matchingLock,
                memberLock,
                sessionLock
        );
    }

    @Bean
    @Primary
    public BlindDateMessageHandler blindDateMessageHandler(
            BlindDateParticipantStorage participantStorage,
            SimpMessagingTemplate messagingTemplate
    ) {
        return new BlindDateMessageHandler(
                participantStorage,
                messagingTemplate
        );
    }

    @Bean
    @Primary
    public BlindDateChoiceHandler blindDateChoiceHandler(
            BlindDateParticipantStorage participantStorage,
            SimpMessagingTemplate messagingTemplate,
            ChatRoomService chatRoomService
    ) {
        return new BlindDateChoiceHandler(
                participantStorage,
                messagingTemplate,
                chatRoomService
        );
    }

    @Bean
    @Primary
    public BlindDateGateway blindDateGateway(
            BlindDateConnectHandler connectHandler,
            BlindDateDisconnectHandler disconnectHandler,
            BlindDateMessageHandler messageHandler,
            BlindDateChoiceHandler choiceHandler
    ) {
        return new BlindDateGateway(
                connectHandler,
                disconnectHandler,
                messageHandler,
                choiceHandler
        );
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws/blinddate")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }
}
