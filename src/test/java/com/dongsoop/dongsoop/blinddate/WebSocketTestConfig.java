package com.dongsoop.dongsoop.blinddate;

import com.dongsoop.dongsoop.blinddate.gateway.BlindDateGateway;
import com.dongsoop.dongsoop.blinddate.handler.BlindDateChoiceHandler;
import com.dongsoop.dongsoop.blinddate.handler.BlindDateConnectHandler;
import com.dongsoop.dongsoop.blinddate.handler.BlindDateDisconnectHandler;
import com.dongsoop.dongsoop.blinddate.handler.BlindDateMessageHandler;
import com.dongsoop.dongsoop.blinddate.lock.BlindDateMatchingLock;
import com.dongsoop.dongsoop.blinddate.lock.BlindDateMemberLock;
import com.dongsoop.dongsoop.blinddate.notification.BlindDateNotification;
import com.dongsoop.dongsoop.blinddate.repository.BlindDateInfoRepositoryImpl;
import com.dongsoop.dongsoop.blinddate.repository.ParticipantInfoRepository;
import com.dongsoop.dongsoop.blinddate.repository.ParticipantInfoRepositoryImpl;
import com.dongsoop.dongsoop.blinddate.repository.SessionInfoRepository;
import com.dongsoop.dongsoop.blinddate.repository.SessionInfoRepositoryImpl;
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
@ComponentScan(basePackages = {"com.dongsoop.dongsoop.jwt", "com.dongsoop.dongsoop.blinddate"})
public class WebSocketTestConfig implements WebSocketMessageBrokerConfigurer {

    @Bean
    @Primary
    public BlindDateInfoRepositoryImpl blindDateInfoRepository() {
        return new BlindDateInfoRepositoryImpl();
    }

    @Bean
    @Primary
    public ParticipantInfoRepository participantInfoRepository() {
        return new ParticipantInfoRepositoryImpl();
    }

    @Bean
    @Primary
    public SessionInfoRepository sessionInfoRepository() {
        return new SessionInfoRepositoryImpl(participantInfoRepository());
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
            ParticipantInfoRepository participantInfoRepository,
            BlindDateInfoRepositoryImpl blindDateInfoRepository,
            BlindDateNotification notification,
            SessionInfoRepository sessionInfoRepository,
            SimpMessagingTemplate messagingTemplate,
            BlindDateTaskScheduler taskScheduler
    ) {
        return new BlindDateServiceImpl(
                participantInfoRepository,
                blindDateInfoRepository,
                notification,
                sessionInfoRepository,
                messagingTemplate,
                taskScheduler
        );
    }

    @Bean
    @Primary
    public BlindDateSessionService blindDateSessionService(
            ParticipantInfoRepository participantInfoRepository,
            BlindDateInfoRepositoryImpl blindDateInfoRepository
    ) {
        return new BlindDateSessionServiceImpl(
                participantInfoRepository,
                blindDateInfoRepository
        );
    }

    @Bean
    @Primary
    public BlindDateConnectHandler blindDateConnectHandler(
            ParticipantInfoRepository participantInfoRepository,
            BlindDateInfoRepositoryImpl blindDateInfoRepository,
            SessionInfoRepository sessionInfoRepository,
            BlindDateService blindDateService,
            BlindDateSessionService sessionService,
            BlindDateSessionScheduler sessionScheduler,
            BlindDateTaskScheduler taskScheduler,
            SimpMessagingTemplate messagingTemplate,
            BlindDateMatchingLock matchingLock,
            BlindDateMemberLock memberLock
    ) {
        return new BlindDateConnectHandler(
                participantInfoRepository,
                blindDateInfoRepository,
                sessionInfoRepository,
                blindDateService,
                sessionService,
                sessionScheduler,
                taskScheduler,
                messagingTemplate,
                matchingLock,
                memberLock
        );
    }

    @Bean
    @Primary
    public BlindDateDisconnectHandler blindDateDisconnectHandler(
            ParticipantInfoRepository participantInfoRepository,
            SessionInfoRepository sessionInfoRepository,
            BlindDateService blindDateService,
            BlindDateMatchingLock matchingLock,
            BlindDateMemberLock memberLock
    ) {
        return new BlindDateDisconnectHandler(
                participantInfoRepository,
                sessionInfoRepository,
                blindDateService,
                matchingLock,
                memberLock
        );
    }

    @Bean
    @Primary
    public BlindDateMessageHandler blindDateMessageHandler(
            ParticipantInfoRepository participantInfoRepository,
            SimpMessagingTemplate messagingTemplate
    ) {
        return new BlindDateMessageHandler(
                participantInfoRepository,
                messagingTemplate
        );
    }

    @Bean
    @Primary
    public BlindDateChoiceHandler blindDateChoiceHandler(
            ParticipantInfoRepository participantInfoRepository,
            SimpMessagingTemplate messagingTemplate,
            ChatRoomService chatRoomService
    ) {
        return new BlindDateChoiceHandler(
                participantInfoRepository,
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
