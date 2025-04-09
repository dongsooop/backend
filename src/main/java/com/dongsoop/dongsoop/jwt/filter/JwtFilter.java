package com.dongsoop.dongsoop.jwt.filter;

import com.dongsoop.dongsoop.exception.domain.jwt.TokenNotFoundException;
import com.dongsoop.dongsoop.jwt.JwtUtil;
import com.dongsoop.dongsoop.jwt.JwtValidator;
import com.dongsoop.dongsoop.member.service.MemberDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private static final String PREFIX = "Bearer";

    private static final Integer TOKEN_START_INDEX = 7;

    private final JwtUtil jwtUtil;

    private final JwtValidator jwtValidator;

    private final MemberDetailsService memberDetailsService;

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

        String name = jwtUtil.getNameByToken(token);

        UserDetails userDetails = memberDetailsService.loadUserByUsername(name);
        Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, userDetails.getPassword());
        SecurityContextHolder.getContext().setAuthentication(auth);

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
