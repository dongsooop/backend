package com.dongsoop.dongsoop.monitoring.config;

import com.dongsoop.dongsoop.monitoring.interceptor.ApiUsageInterceptor;
import com.dongsoop.dongsoop.monitoring.service.ApiUsageRecorder;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@ConditionalOnProperty(name = "monitoring.usage.enabled", havingValue = "true")
public class MonitoringWebConfig implements WebMvcConfigurer {

    private final ApiUsageRecorder recorder;
    private final List<String> systemUriPatterns;
    private final Set<Long> excludedMemberIds;

    public MonitoringWebConfig(ApiUsageRecorder recorder,
                               @Value("${monitoring.usage.system-uris:}") String[] systemUriPatterns,
                               @Value("${monitoring.usage.exclude-member-ids:}") String[] excludedMemberIds) {
        this.recorder = recorder;
        this.systemUriPatterns = Arrays.stream(systemUriPatterns).map(String::trim).filter(p -> !p.isEmpty()).toList();
        this.excludedMemberIds = Arrays.stream(excludedMemberIds).map(String::trim).filter(p -> !p.isEmpty())
                .map(Long::valueOf).collect(Collectors.toSet());
    }

    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        // /error 는 실패한 요청이 재전달되는 경로라 여기서 세면 같은 요청이 두 번 잡히고,
        // /health 는 도커 헬스체크가 몇 초마다 부르는 경로라 사용량을 왜곡한다
        registry.addInterceptor(new ApiUsageInterceptor(recorder, systemUriPatterns, excludedMemberIds))
                .addPathPatterns("/**")
                .excludePathPatterns("/error", "/health");
    }
}
