package com.dongsoop.dongsoop.jwt.filter;

import com.dongsoop.dongsoop.jwt.JwtUtil;
import com.dongsoop.dongsoop.jwt.JwtValidator;
import com.dongsoop.dongsoop.jwt.exception.NotAccessTokenException;
import com.dongsoop.dongsoop.jwt.exception.TokenExpiredException;
import com.dongsoop.dongsoop.jwt.exception.TokenMalformedException;
import com.dongsoop.dongsoop.jwt.exception.TokenNotFoundException;
import com.dongsoop.dongsoop.jwt.exception.TokenRoleNotAvailableException;
import com.dongsoop.dongsoop.jwt.exception.TokenSignatureException;
import com.dongsoop.dongsoop.jwt.exception.TokenUnsupportedException;
import io.jsonwebtoken.Claims;
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
    private final String[] ignorePaths;

    public JwtFilter(JwtUtil jwtUtil,
                     JwtValidator jwtValidator,
                     @Qualifier("handlerExceptionResolver") HandlerExceptionResolver exceptionResolver,
                     @Value("${authentication.filter.ignore.paths}") String[] ignorePaths) {
        this.jwtUtil = jwtUtil;
        this.jwtValidator = jwtValidator;
        this.exceptionResolver = exceptionResolver;
        this.ignorePaths = ignorePaths;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) {
        try {
            String token = extractTokenFromHeader(request);
            Claims claims = jwtUtil.getClaims(token);
            jwtValidator.validate(claims);
            jwtValidator.validateAccessToken(claims);
            setAuthentication(token);
        } catch (TokenNotFoundException exception) {
            log.debug("Member doesn't have token: {}", exception.getMessage(), exception);
        } catch (TokenMalformedException | TokenExpiredException | NotAccessTokenException |
                 TokenSignatureException | TokenRoleNotAvailableException | TokenUnsupportedException exception) {
            log.error("Token validation failed: {}", exception.getMessage(), exception);
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
