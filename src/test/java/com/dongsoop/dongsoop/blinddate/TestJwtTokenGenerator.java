package com.dongsoop.dongsoop.blinddate;

import com.dongsoop.dongsoop.jwt.TokenGenerator;
import com.dongsoop.dongsoop.role.entity.RoleType;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
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
        return tokenGenerator.generateAccessToken(
                new UsernamePasswordAuthenticationToken(memberId, List.of(RoleType.USER_ROLE)));
    }
}
