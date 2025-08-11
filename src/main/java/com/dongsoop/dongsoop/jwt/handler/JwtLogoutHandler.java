package com.dongsoop.dongsoop.jwt.handler;

import com.dongsoop.dongsoop.jwt.exception.DeviceInformationNotIncludedInHeaderException;
import com.dongsoop.dongsoop.member.service.MemberService;
import com.dongsoop.dongsoop.memberdevice.entity.MemberDevice;
import com.dongsoop.dongsoop.memberdevice.exception.UnregisteredDeviceException;
import com.dongsoop.dongsoop.memberdevice.repository.MemberDeviceRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class JwtLogoutHandler implements LogoutHandler {

    private final MemberDeviceRepository memberDeviceRepository;
    private final MemberService memberService;

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        // TODO: 화이트리스트 추가 시 메모리에 유효한 토큰 만료일 갱신(화이트리스트)
        String deviceToken = request.getHeader("Device-Token");
        if (!StringUtils.hasText(deviceToken)) {
            throw new DeviceInformationNotIncludedInHeaderException();
        }
        Long memberId = memberService.getMemberIdByAuthentication();

        MemberDevice device = memberDeviceRepository.findByMemberIdAndDeviceToken(memberId, deviceToken)
                .orElseThrow(UnregisteredDeviceException::new);

        memberDeviceRepository.delete(device);
    }
}
