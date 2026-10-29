package com.dongsoop.dongsoop.chat;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.dongsoop.dongsoop.chat.exception.UnauthorizedChatAccessException;
import com.dongsoop.dongsoop.chat.session.WebSocketSessionManager;
import com.dongsoop.dongsoop.common.handler.websocket.StompHandler;
import com.dongsoop.dongsoop.jwt.JwtUtil;
import com.dongsoop.dongsoop.jwt.JwtValidator;
import com.dongsoop.dongsoop.jwt.dto.AuthenticationInformationByToken;
import com.dongsoop.dongsoop.jwt.exception.BlacklistedTokenException;
import com.dongsoop.dongsoop.jwt.service.DeviceBlacklistService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class StompConnectionContractTest {
  @Mock JwtValidator validator;
  @Mock JwtUtil jwt;
  @Mock WebSocketSessionManager sessions;
  @Mock DeviceBlacklistService blacklist;
  @InjectMocks StompHandler service;

  @AfterEach
  void clear() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void connectValidatesTokenAndSetsPrincipalContextAndSessionOwner() {
    Claims claims = authenticatedToken();
    StompHeaderAccessor headers = connect("Bearer signed-token");
    headers.setSessionId("session-7");
    headers.setSessionAttributes(new HashMap<>());
    Message<byte[]> message = message(headers);
    assertThat(service.preSend(message, null)).isSameAs(message);
    verify(validator).validate(claims);
    assertThat(headers.getUser().getName()).isEqualTo("7");
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    assertThat(authentication.getAuthorities())
        .extracting("authority")
        .containsExactly("ROLE_USER");
    assertThat(headers.getSessionAttributes()).containsEntry("memberId", 7L);
    verify(sessions).addUserSession(7L, "session-7");
  }

  @Test
  void validConnectWithoutSessionMetadataStillAuthenticates() {
    authenticatedToken();
    StompHeaderAccessor headers = connect("Bearer signed-token");
    service.preSend(message(headers), null);
    assertThat(headers.getUser().getName()).isEqualTo("7");
    verifyNoInteractions(sessions);
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {" ", "Bearer", "Bearer ", "Basic credential"})
  void invalidAuthorizationNeverParsesTokenOrRegistersSession(String header) {
    Message<byte[]> message = message(connect(header));
    assertThatThrownBy(() -> service.preSend(message, null))
        .isInstanceOf(UnauthorizedChatAccessException.class);
    verifyNoInteractions(jwt, validator, sessions);
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
  }

  @Test
  void tokenParsingFailureCannotAuthenticateOrRegisterSession() {
    when(jwt.getClaims("signed-token")).thenThrow(new IllegalStateException("expired"));
    Message<byte[]> message = message(connect("Bearer signed-token"));
    assertThatThrownBy(() -> service.preSend(message, null))
        .isInstanceOf(UnauthorizedChatAccessException.class);
    verifyNoInteractions(sessions, validator);
  }

  @Test
  void blacklistedDeviceTokenCannotConnectOrRegisterSession() {
    Date issuedAt = new Date(1_700_000_000_000L);
    Claims claims =
        Jwts.claims().subject("7").add(JwtUtil.DEVICE_ID_CLAIM, 5L).issuedAt(issuedAt).build();
    when(jwt.getClaims("signed-token")).thenReturn(claims);
    doThrow(new BlacklistedTokenException())
        .when(blacklist)
        .validateNotBlacklisted(5L, issuedAt.toInstant());
    Message<byte[]> message = message(connect("Bearer signed-token"));
    assertThatThrownBy(() -> service.preSend(message, null))
        .isInstanceOf(UnauthorizedChatAccessException.class);
    verifyNoInteractions(sessions);
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
  }

  @Test
  void principalResolutionFailureIsTranslatedAndCannotRegisterSession() {
    Claims claims = Jwts.claims().subject("7").build();
    when(jwt.getClaims("signed-token")).thenReturn(claims);
    when(jwt.getTokenInformation("signed-token"))
        .thenThrow(new IllegalStateException("invalid role"));
    Message<byte[]> message = message(connect("Bearer signed-token"));
    assertThatThrownBy(() -> service.preSend(message, null))
        .isInstanceOf(UnauthorizedChatAccessException.class);
    verifyNoInteractions(sessions);
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
  }

  @Test
  void disconnectOnlyRemovesPresentSessionAndOtherCommandsAreUnchanged() {
    StompHeaderAccessor disconnect = StompHeaderAccessor.create(StompCommand.DISCONNECT);
    disconnect.setSessionId("session-7");
    service.preSend(message(disconnect), null);
    service.preSend(message(StompHeaderAccessor.create(StompCommand.DISCONNECT)), null);
    Message<byte[]> subscribe = message(StompHeaderAccessor.create(StompCommand.SUBSCRIBE));
    assertThat(service.preSend(subscribe, null)).isSameAs(subscribe);
    Message<byte[]> plain = MessageBuilder.withPayload(new byte[0]).build();
    assertThat(service.preSend(plain, null)).isSameAs(plain);
    verify(sessions).removeSession("session-7");
    verifyNoMoreInteractions(sessions);
    verifyNoInteractions(jwt, validator);
  }

  private Claims authenticatedToken() {
    Claims claims = Jwts.claims().subject("7").build();
    when(jwt.getClaims("signed-token")).thenReturn(claims);
    when(jwt.getTokenInformation("signed-token"))
        .thenReturn(
            new AuthenticationInformationByToken(
                7L, List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    return claims;
  }

  private StompHeaderAccessor connect(String authorization) {
    StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.CONNECT);
    if (authorization != null) {
      headers.setNativeHeader("Authorization", authorization);
    }
    return headers;
  }

  private Message<byte[]> message(StompHeaderAccessor headers) {
    headers.setLeaveMutable(true);
    return MessageBuilder.createMessage(new byte[0], headers.getMessageHeaders());
  }
}
