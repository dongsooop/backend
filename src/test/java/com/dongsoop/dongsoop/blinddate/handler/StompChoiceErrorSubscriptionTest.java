package com.dongsoop.dongsoop.blinddate.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dongsoop.dongsoop.chat.exception.UnauthorizedChatAccessException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.security.Principal;
import java.util.List;

class StompChoiceErrorSubscriptionTest {
    private final BlindDateStompAuthorizationInterceptor interceptor =
            new BlindDateStompAuthorizationInterceptor();
    private static final String OWN_TOPIC =
            "/topic/blinddate/session/session-1/member/1/choice-error";

    @Test
    void authenticatedOwnerCanSubscribeBeforeChoicesOpen() {
        // 참가자 저장소와 선택 기간에 의존하지 않고 인증된 수신자의 사전 구독을 허용한다.
        assertAllowed(OWN_TOPIC, authenticated(1L));
    }

    @Test
    void anotherMemberCannotSubscribe() {
        assertDenied(OWN_TOPIC, authenticated(2L));
    }

    @Test
    void missingPrincipalCannotSubscribe() {
        assertDenied(OWN_TOPIC, null);
    }

    @Test
    void unauthenticatedPrincipalCannotSubscribe() {
        assertDenied(OWN_TOPIC, new UsernamePasswordAuthenticationToken(1L, null));
    }

    @Test
    void arbitraryPrincipalCannotSubscribe() {
        assertDenied(OWN_TOPIC, () -> "1");
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "/topic/**",
                "/topic/*/**",
                "/topic/blinddate/**",
                "/topic/blinddate/session/session-1/member/*/choice-error",
                "/topic/blinddate/session/session-1/member/?/choice-error",
                "/topic/blinddate/session/{session}/member/{member}/choice-error"
            })
    void patternsCannotBypassOwnershipCheck(String destination) {
        assertDenied(destination, authenticated(1L));
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "/topic/blinddate/session/session-1/participants",
                "/topic/blinddate/session/session-1/start",
                "/user/queue/blinddate/join",
                "/topic/chat/**",
                "/topic/blinddate/join"
            })
    void otherSubscriptionsKeepExistingBehavior(String destination) {
        assertAllowed(destination, authenticated(1L));
    }

    @ParameterizedTest
    @ValueSource(longs = {1L, 2L})
    void clientsCannotPublishOwnOrAnotherMembersErrors(long member) {
        var headers = StompHeaderAccessor.create(StompCommand.SEND);
        headers.setDestination(OWN_TOPIC);
        headers.setUser(authenticated(member));
        headers.setLeaveMutable(true);
        assertThatThrownBy(
                        () ->
                                interceptor.preSend(
                                        MessageBuilder.createMessage(
                                                new byte[0], headers.getMessageHeaders()),
                                        null))
                .isInstanceOf(UnauthorizedChatAccessException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/app/blinddate/choice", "/app/blinddate/message"})
    void applicationSendKeepsExistingBehavior(String destination) {
        var headers = StompHeaderAccessor.create(StompCommand.SEND);
        headers.setDestination(destination);
        headers.setUser(authenticated(1L));
        headers.setLeaveMutable(true);
        var result =
                interceptor.preSend(
                        MessageBuilder.createMessage(new byte[0], headers.getMessageHeaders()),
                        null);
        assertThat(StompHeaderAccessor.wrap(result).getDestination()).isEqualTo(destination);
    }

    private Principal authenticated(Long member) {
        return new UsernamePasswordAuthenticationToken(member, null, List.of());
    }

    private void assertAllowed(String destination, Principal principal) {
        var result = interceptor.preSend(subscription(destination, principal), null);
        var headers = StompHeaderAccessor.wrap(result);
        assertThat(headers.getCommand()).isEqualTo(StompCommand.SUBSCRIBE);
        assertThat(headers.getDestination()).isEqualTo(destination);
    }

    private void assertDenied(String destination, Principal principal) {
        assertThatThrownBy(() -> interceptor.preSend(subscription(destination, principal), null))
                .isInstanceOf(UnauthorizedChatAccessException.class);
    }

    private Message<byte[]> subscription(String destination, Principal principal) {
        var headers = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        headers.setDestination(destination);
        headers.setUser(principal);
        headers.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], headers.getMessageHeaders());
    }
}
