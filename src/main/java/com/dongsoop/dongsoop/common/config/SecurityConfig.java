package com.dongsoop.dongsoop.common.config;

import com.dongsoop.dongsoop.common.handler.authentication.CustomAccessDeniedHandler;
import com.dongsoop.dongsoop.common.handler.authentication.CustomAuthenticationEntryPoint;
import com.dongsoop.dongsoop.jwt.filter.JwtFilter;
import com.dongsoop.dongsoop.oauth.handler.OAuth2LoginFailureHandler;
import com.dongsoop.dongsoop.oauth.handler.OAuth2LoginSuccessHandler;
import com.dongsoop.dongsoop.oauth.service.CustomOAuth2UserService;
import com.dongsoop.dongsoop.role.entity.RoleType;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;

    private final LogoutHandler logoutHandler;

    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;

    private final CustomAccessDeniedHandler customAccessDeniedHandler;

    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
    private final OAuth2LoginFailureHandler oAuth2LoginFailureHandler;

    @Value("${authentication.path.all}")
    private String[] allowedPaths;

    @Value("${authentication.path.user}")
    private String[] userAllowedPaths;

    @Value("${authentication.path.admin}")
    private String[] adminAllowedPaths;

    @Value("${authentication.origins}")
    private String[] allowedOrigins;

    @Value("${authentication.methods}")
    private String[] allowedMethod;

    @Bean
    protected SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(authorizationManagerRequestMatcherRegistry ->
                        authorizationManagerRequestMatcherRegistry
                                .requestMatchers(userAllowedPaths).hasRole(RoleType.USER.name())
                                .requestMatchers(adminAllowedPaths).hasRole(RoleType.ADMIN.name())
                                .requestMatchers(allowedPaths).permitAll()
                                .anyRequest().authenticated()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .addLogoutHandler(logoutHandler))
                .httpBasic(AbstractHttpConfigurer::disable) // 기본 인증 기능 제거
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable) // JWT를 사용하기 때문에 form login 비활성화
                .addFilterBefore(jwtFilter, LogoutFilter.class)
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                        .successHandler(oAuth2LoginSuccessHandler)
                        .failureHandler(oAuth2LoginFailureHandler))
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(customAuthenticationEntryPoint) // 401 에러 응답 처리
                        .accessDeniedHandler(customAccessDeniedHandler)) // 403 에러 응답 처리
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowCredentials(true); // cors 요청을 허용할 때 인증정보를 함께 보낼지 여부
        config.setAllowedOrigins(List.of(allowedOrigins));
        config.setAllowedMethods(List.of(allowedMethod));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("*"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
