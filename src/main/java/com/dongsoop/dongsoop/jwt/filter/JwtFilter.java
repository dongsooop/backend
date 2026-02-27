package com.dongsoop.dongsoop.jwt.filter;


import com.dongsoop.dongsoop.jwt.JwtUtil;
import com.dongsoop.dongsoop.jwt.JwtValidator;
import com.dongsoop.dongsoop.jwt.exception.JWTException;
import com.dongsoop.dongsoop.jwt.exception.TokenNotFoundException;
import com.dongsoop.dongsoop.jwt.service.DeviceBlacklistService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

@Component
@Slf4j
public class JwtFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final int TOKEN_START_INDEX = BEARER_PREFIX.length();
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private final JwtUtil jwtUtil;
    private final JwtValidator jwtValidator;
    private final DeviceBlacklistService deviceBlacklistService;
    private final HandlerExceptionResolver exceptionResolver;
    private final String[] ignorePaths;

    public JwtFilter(JwtUtil jwtUtil,
                     JwtValidator jwtValidator,
                     DeviceBlacklistService deviceBlacklistService,
                     @Qualifier("handlerExceptionResolver") HandlerExceptionResolver exceptionResolver,
                     @Value("${authentication.filter.ignore.paths}") String[] ignorePaths) {
        this.jwtUtil = jwtUtil;
        this.jwtValidator = jwtValidator;
        this.deviceBlacklistService = deviceBlacklistService;
        this.exceptionResolver = exceptionResolver;
        this.ignorePaths = ignorePaths;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws IOException {
        try {
            String token = extractTokenFromHeader(request);
            Claims claims = jwtUtil.getClaims(token);
            jwtValidator.validate(claims);
            jwtValidator.validateAccessToken(claims);

            // deviceId claim이 있는 경우에만 블랙리스트 검사 (없으면 기존 토큰으로 간주하여 스킵)
            Long deviceId = claims.get(JwtUtil.DEVICE_ID_CLAIM, Long.class);
            if (deviceId != null) {
                deviceBlacklistService.validateNotBlacklisted(deviceId, claims.getIssuedAt());
            }

            setAuthentication(token);
            log.debug("JWT token validation successful");
        } catch (TokenNotFoundException exception) {
            log.debug("No JWT token found in request: {}", exception.getMessage());
        } catch (JWTException exception) {
            log.warn("JWT token validation failed: {}", exception.getMessage());
            exceptionResolver.resolveException(request, response, null, exception);
            return;
        }

        try {
            filterChain.doFilter(request, response);
        } catch (Exception exception) {
            log.error("Filter chain processing failed: {}", exception.getMessage(), exception);
            exceptionResolver.resolveException(request, response, null, exception);
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();

        return Arrays.stream(ignorePaths)
                .anyMatch(allowPath -> PATH_MATCHER.match(allowPath, path));
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

    private void setAuthentication(String token) {
        Authentication auth = jwtUtil.getAuthenticationByToken(token);

        SecurityContext context = SecurityContextHolder.getContext();
        context.setAuthentication(auth);

        log.info("SecurityContextHolder에 인증 정보 저장 : {}", context.getAuthentication());
    }
}
