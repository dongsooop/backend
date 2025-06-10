package com.dongsoop.dongsoop.jwt.filter;

import com.dongsoop.dongsoop.exception.domain.jwt.TokenNotFoundException;
import com.dongsoop.dongsoop.jwt.JwtUtil;
import com.dongsoop.dongsoop.jwt.JwtValidator;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
    private final HandlerExceptionResolver exceptionResolver;
    private final String[] allowedPaths;

    public JwtFilter(JwtUtil jwtUtil,
                     JwtValidator jwtValidator,
                     @Qualifier("handlerExceptionResolver") HandlerExceptionResolver exceptionResolver,
                     @Value("${authentication.path.all}") String[] allowedPaths) {
        this.jwtUtil = jwtUtil;
        this.jwtValidator = jwtValidator;
        this.exceptionResolver = exceptionResolver;
        this.allowedPaths = allowedPaths;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) {
        try {
            String token = extractTokenFromHeader(request);
            validateAndSetAuthentication(token);
            filterChain.doFilter(request, response);
        } catch (Exception exception) {
            handleFilterException(request, response, exception); // 예외 발생 시 MVC 레이어로 예외 처리 위임
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();

        return Arrays.stream(allowedPaths)
                .anyMatch(allowPath -> PATH_MATCHER.match(allowPath, path));
    }

    private String extractTokenFromHeader(HttpServletRequest request) {
        String tokenHeader = request.getHeader(AUTHORIZATION_HEADER);

        if (!StringUtils.hasText(tokenHeader) ||
                tokenHeader.length() <= TOKEN_START_INDEX ||
                !tokenHeader.startsWith(BEARER_PREFIX)) {
            throw new TokenNotFoundException();
        }

        return tokenHeader.substring(TOKEN_START_INDEX);
    }

    private void validateAndSetAuthentication(String token) {
        jwtValidator.validate(token);
        Authentication auth = jwtUtil.getAuthenticationByToken(token);

        SecurityContext context = SecurityContextHolder.getContext();
        context.setAuthentication(auth);

        log.info("SecurityContextHolder에 인증 정보 저장 : {}", context.getAuthentication());
    }

    private void handleFilterException(
            HttpServletRequest request,
            HttpServletResponse response,
            Exception exception
    ) {
        log.error("JWT Filter processing failed: {}", exception.getMessage(), exception);
        exceptionResolver.resolveException(request, response, null, exception);
    }
}
