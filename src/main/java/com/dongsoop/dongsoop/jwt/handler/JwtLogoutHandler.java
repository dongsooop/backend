package com.dongsoop.dongsoop.jwt.handler;

import com.dongsoop.dongsoop.jwt.JwtUtil;
import com.dongsoop.dongsoop.jwt.exception.DeviceInformationNotIncludedInHeaderException;
import com.dongsoop.dongsoop.jwt.exception.TokenNotFoundException;
import com.dongsoop.dongsoop.memberdevice.entity.MemberDevice;
import com.dongsoop.dongsoop.memberdevice.exception.UnregisteredDeviceException;
import com.dongsoop.dongsoop.memberdevice.repository.MemberDeviceRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class JwtLogoutHandler implements LogoutHandler {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final int TOKEN_START_INDEX = BEARER_PREFIX.length();

    private final MemberDeviceRepository memberDeviceRepository;
    private final JwtUtil jwtUtil;

    @Override
    @Transactional
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        // TODO: 화이트리스트 추가 시 메모리에 유효한 토큰 만료일 갱신(화이트리스트)
        String deviceToken = request.getHeader("Device-Token");
        if (!StringUtils.hasText(deviceToken)) {
            throw new DeviceInformationNotIncludedInHeaderException();
        }

        String token = extractTokenFromHeader(request);
        Claims claims = jwtUtil.getClaims(token);
        Long memberId = Long.valueOf(claims.getSubject());

        MemberDevice device = memberDeviceRepository.findByMemberIdAndDeviceToken(memberId, deviceToken)
                .orElseThrow(UnregisteredDeviceException::new);

        device.bindMember(null);
    }

    private String extractTokenFromHeader(HttpServletRequest request) throws TokenNotFoundException {
        String tokenHeader = request.getHeader(AUTHORIZATION_HEADER);

        if (!StringUtils.hasText(tokenHeader) ||
                tokenHeader.length() <= TOKEN_START_INDEX ||
                !tokenHeader.startsWith(BEARER_PREFIX)) {
            throw new TokenNotFoundException();
        }

        return tokenHeader.substring(TOKEN_START_INDEX);
    }
}
