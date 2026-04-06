package com.dongsoop.dongsoop.memberdevice.filter;

import com.dongsoop.dongsoop.memberdevice.service.MemberDeviceService;
import com.dongsoop.dongsoop.memberdevice.util.DeviceUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
@Slf4j
public class DeviceLastAccessFilter extends OncePerRequestFilter {

    private final MemberDeviceService memberDeviceService;
    private final DeviceUtil deviceUtil;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        Long deviceId = deviceUtil.getDeviceIdFromContext();
        if (deviceId != null) {
            try {
                memberDeviceService.updateLastAccessAsync(deviceId);
            } catch (Exception e) {
                log.debug("Failed to update device last access for deviceId={}: {}", deviceId, e.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }
}
