package com.dongsoop.dongsoop.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;

@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private static final String PREFIX = "Bearer";

    private static final Integer TOKEN_START_INDEX = 7;

    private final JwtUtil jwtUtil;

    private final AntPathMatcher matcher = new AntPathMatcher();

    @Value("${authentication.path.all}")
    private String[] allowedPaths;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String tokenHeader = request.getHeader("Authorization");
        String token = resolveToken(tokenHeader);
        validateToken(token);

        Authentication auth = jwtUtil.getAuthentication(token);
        SecurityContextHolder.getContext().setAuthentication(auth);

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();

        return Arrays.stream(allowedPaths)
                .anyMatch(allowPath -> matcher.match(allowPath, path));
    }

    private void validateToken(String token) {
        jwtUtil.validate(token);
    }

    private String resolveToken(String tokenHeader) {
        if (!StringUtils.hasText(tokenHeader) ||
                tokenHeader.length() <= TOKEN_START_INDEX ||
                !tokenHeader.startsWith(PREFIX)) {
            throw new CustomJwtException(ErrorCode.TOKEN_NOT_FOUND_EXCEPTION);
        }

        return tokenHeader.substring(TOKEN_START_INDEX);
    }

}
