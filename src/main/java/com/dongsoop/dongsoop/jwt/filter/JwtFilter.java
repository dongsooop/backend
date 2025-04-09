package com.dongsoop.dongsoop.jwt.filter;

import com.dongsoop.dongsoop.exception.domain.jwt.TokenNotFoundException;
import com.dongsoop.dongsoop.jwt.JwtUtil;
import com.dongsoop.dongsoop.jwt.JwtValidator;
import com.dongsoop.dongsoop.jwt.dto.AuthenticationInformationByToken;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Slf4j
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private static final String PREFIX = "Bearer";

    private static final Integer TOKEN_START_INDEX = 7;

    private final JwtUtil jwtUtil;

    private final JwtValidator jwtValidator;

    private final AntPathMatcher matcher = new AntPathMatcher();

    @Value("${authentication.path.all}")
    private String[] allowedPaths;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        String tokenHeader = request.getHeader("Authorization");
        String token = resolveToken(tokenHeader);
        jwtValidator.validate(token);

        AuthenticationInformationByToken authenticationInformation = jwtUtil.getTokenInformation(token);
        Long id = authenticationInformation.getId();
        Collection<? extends GrantedAuthority> role = authenticationInformation.getRole();

        Authentication auth = new UsernamePasswordAuthenticationToken(id, null, role);

        SecurityContext context = SecurityContextHolder.getContext();
        context.setAuthentication(auth);

        log.info("SecurityContextHolder에 인증 정보 저장 : {}", context.getAuthentication());

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();

        return Arrays.stream(allowedPaths)
                .anyMatch(allowPath -> matcher.match(allowPath, path));
    }

    private String resolveToken(String tokenHeader) {
        if (!StringUtils.hasText(tokenHeader) ||
                tokenHeader.length() <= TOKEN_START_INDEX ||
                !tokenHeader.startsWith(PREFIX)) {
            throw new TokenNotFoundException();
        }

        return tokenHeader.substring(TOKEN_START_INDEX);
    }

}
