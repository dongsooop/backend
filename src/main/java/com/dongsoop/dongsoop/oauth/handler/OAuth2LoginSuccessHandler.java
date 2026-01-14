package com.dongsoop.dongsoop.oauth.handler;

import com.dongsoop.dongsoop.jwt.TokenGenerator;
import com.dongsoop.dongsoop.oauth.dto.CustomOAuth2User;
import com.dongsoop.dongsoop.role.entity.RoleType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@Slf4j
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private static final String SOCIAL_TOKEN_PARAM = "socialToken";

    private final TokenGenerator tokenGenerator;

    @Value("${oauth.authorize-uri}")
    private String targetUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();
        List<GrantedAuthority> authorityList = oAuth2User.getRoleTypeList().stream().map(RoleType::getAuthority)
                .toList();
        Authentication auth = new UsernamePasswordAuthenticationToken(oAuth2User.getMemberId(), null, authorityList);

        String targetUrl = determineTargetUrl(auth);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
        log.info("OAuth2 login success. memberId: {}", oAuth2User.getMemberId());
    }

    protected String determineTargetUrl(Authentication authentication) {

        String socialToken = tokenGenerator.generateSocialToken(authentication);

        return UriComponentsBuilder.fromUriString(targetUrl)
                .queryParam(SOCIAL_TOKEN_PARAM, socialToken)
                .build().toUriString();
    }
}
