package com.dongsoop.dongsoop.blinddate;

import com.dongsoop.dongsoop.jwt.TokenGenerator;
import com.dongsoop.dongsoop.role.entity.RoleType;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

/**
 * 테스트용 JWT 토큰 생성기
 */
@Component
public class TestJwtTokenGenerator {

    private final TokenGenerator tokenGenerator;

    public TestJwtTokenGenerator(TokenGenerator tokenGenerator) {
        this.tokenGenerator = tokenGenerator;
    }

    /**
     * 테스트용 Access Token 생성
     *
     * @param memberId 회원 ID
     * @return JWT Access Token
     */
    public String generateAccessToken(Long memberId) {
        return generateAccessToken(memberId, null);
    }

    /**
     * 테스트용 Access Token 생성 (deviceId 포함)
     *
     * @param memberId 회원 ID
     * @param deviceId 디바이스 ID (null 허용)
     * @return JWT Access Token
     */
    public String generateAccessToken(Long memberId, Long deviceId) {
        List<GrantedAuthority> auth = List.of(new SimpleGrantedAuthority(RoleType.USER_ROLE));
        UsernamePasswordAuthenticationToken user = new UsernamePasswordAuthenticationToken(memberId, null, auth);

        return tokenGenerator.generateAccessToken(user, deviceId);
    }
}
