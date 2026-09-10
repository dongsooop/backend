package com.dongsoop.dongsoop.eclass.service;

import com.dongsoop.dongsoop.common.exception.authentication.NotAuthenticationException;
import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.member.service.MemberService;
import com.dongsoop.dongsoop.memberdevice.entity.MemberDevice;
import com.dongsoop.dongsoop.memberdevice.exception.UnregisteredDeviceException;
import com.dongsoop.dongsoop.memberdevice.service.NoticePreferenceDeviceResolver;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 이클래스 표면에서 기기 헤더를 해석하고, 그 기기를 만질 자격이 있는지까지 함께 판단한다.
 *
 * <p>{@code /eclass/**}는 비회원도 쓸 수 있어야 해서 인증 없이 열려 있고, 기기는 헤더의 fid/토큰으로만
 * 식별된다. 그런데 기기 식별자는 인증 수단이 아니므로 남의 식별자를 헤더에 넣는 것만으로 그 사람의
 * 연동과 과제에 접근할 수 있게 된다. 그래서 회원에게 묶인 기기는 로그인한 회원이 그 주인일 때만 허용하고,
 * 비회원 기기는 식별자를 가진 사람이 곧 주인이므로 그대로 허용한다.
 */
@Component
@RequiredArgsConstructor
public class EclassDeviceAccessor {

    private final NoticePreferenceDeviceResolver deviceResolver;
    private final MemberService memberService;

    /**
     * 헤더가 가리키는 기기 중, 요청자가 접근할 자격이 있는 기기만 돌려준다.
     */
    public Optional<MemberDevice> resolveAccessible(String fid, String deviceToken) {
        return resolve(fid, deviceToken)
                .filter(this::isAccessible);
    }

    /**
     * 인증된 회원 화면 전용. 헤더가 가리키는 기기가 그 회원의 것일 때만 돌려준다.
     */
    public Optional<MemberDevice> resolveOwnedBy(Long memberId, String fid, String deviceToken) {
        return resolve(fid, deviceToken)
                .filter(device -> isOwnedBy(device, memberId));
    }

    private boolean isAccessible(MemberDevice device) {
        Member owner = device.getMember();
        if (owner == null) {
            return true;
        }

        return currentMemberId()
                .map(owner.getId()::equals)
                .orElse(false);
    }

    private boolean isOwnedBy(MemberDevice device, Long memberId) {
        Member owner = device.getMember();

        return owner != null && owner.getId().equals(memberId);
    }

    private Optional<MemberDevice> resolve(String fid, String deviceToken) {
        try {
            return Optional.of(deviceResolver.resolve(fid, deviceToken));
        } catch (UnregisteredDeviceException exception) {
            return Optional.empty();
        }
    }

    private Optional<Long> currentMemberId() {
        try {
            return Optional.of(memberService.getMemberIdByAuthentication());
        } catch (NotAuthenticationException exception) {
            return Optional.empty();
        }
    }
}
