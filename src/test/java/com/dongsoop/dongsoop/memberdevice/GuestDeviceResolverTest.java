package com.dongsoop.dongsoop.memberdevice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.memberdevice.entity.MemberDevice;
import com.dongsoop.dongsoop.memberdevice.entity.MemberDeviceType;
import com.dongsoop.dongsoop.memberdevice.exception.UnregisteredDeviceException;
import com.dongsoop.dongsoop.memberdevice.repository.MemberDeviceRepository;
import com.dongsoop.dongsoop.memberdevice.service.GuestDeviceResolver;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * GuestDeviceResolver의 네 가지 거부 조건(blank/null 토큰, 미등록 토큰, 회원 바인딩 토큰)과
 * fid 우선 조회·폴백 동작을 검증한다. 통합 테스트(GuestDepartmentTest)는 미등록/회원 바인딩 두 케이스만
 * 우회적으로 커버하므로, blank/null 케이스가 빠지지 않도록 단위 테스트로 전부 모은다.
 */
@ExtendWith(MockitoExtension.class)
class GuestDeviceResolverTest {

    @Mock
    private MemberDeviceRepository memberDeviceRepository;

    private GuestDeviceResolver resolver;

    @Test
    @DisplayName("fid와 토큰이 둘 다 없으면 거부한다")
    void rejects_null_token() {
        resolver = new GuestDeviceResolver(memberDeviceRepository);

        assertThatThrownBy(() -> resolver.resolve(null, null))
                .isInstanceOf(UnregisteredDeviceException.class);
    }

    @Test
    @DisplayName("fid 없이 공백 토큰만 있으면 거부한다")
    void rejects_blank_token() {
        resolver = new GuestDeviceResolver(memberDeviceRepository);

        assertThatThrownBy(() -> resolver.resolve(null, "   "))
                .isInstanceOf(UnregisteredDeviceException.class);
    }

    @Test
    @DisplayName("존재하지 않는 토큰은 거부한다")
    void rejects_unknown_token() {
        resolver = new GuestDeviceResolver(memberDeviceRepository);
        given(memberDeviceRepository.findByDeviceToken(anyString())).willReturn(Optional.empty());

        assertThatThrownBy(() -> resolver.resolve(null, "no-such-token"))
                .isInstanceOf(UnregisteredDeviceException.class);
    }

    @Test
    @DisplayName("회원에 바인딩된 디바이스 토큰은 거부한다")
    void rejects_member_bound_device() {
        resolver = new GuestDeviceResolver(memberDeviceRepository);
        MemberDevice bound = MemberDevice.builder()
                .deviceToken("token-bound")
                .memberDeviceType(MemberDeviceType.ANDROID)
                .member(org.mockito.Mockito.mock(Member.class))
                .build();
        given(memberDeviceRepository.findByDeviceToken("token-bound")).willReturn(Optional.of(bound));

        assertThatThrownBy(() -> resolver.resolve(null, "token-bound"))
                .isInstanceOf(UnregisteredDeviceException.class);
    }

    @Test
    @DisplayName("fid로 찾아지면 deviceToken은 조회하지 않는다")
    void resolves_by_fid_without_touching_device_token_lookup() {
        resolver = new GuestDeviceResolver(memberDeviceRepository);
        MemberDevice device = MemberDevice.builder()
                .deviceToken("token-current")
                .fid("fid-known")
                .memberDeviceType(MemberDeviceType.ANDROID)
                .build();
        given(memberDeviceRepository.findByFid("fid-known")).willReturn(Optional.of(device));

        MemberDevice resolved = resolver.resolve("fid-known", "token-current");

        assertThat(resolved).isEqualTo(device);
        org.mockito.Mockito.verify(memberDeviceRepository, org.mockito.Mockito.never())
                .findByDeviceToken(anyString());
    }

    @Test
    @DisplayName("fid로 못 찾으면 deviceToken으로 폴백한다")
    void falls_back_to_device_token_when_fid_not_found() {
        resolver = new GuestDeviceResolver(memberDeviceRepository);
        MemberDevice device = MemberDevice.builder()
                .deviceToken("token-legacy")
                .memberDeviceType(MemberDeviceType.ANDROID)
                .build();
        given(memberDeviceRepository.findByFid("fid-unknown")).willReturn(Optional.empty());
        given(memberDeviceRepository.findByDeviceToken("token-legacy")).willReturn(Optional.of(device));

        MemberDevice resolved = resolver.resolve("fid-unknown", "token-legacy");

        assertThat(resolved).isEqualTo(device);
    }

    @Test
    @DisplayName("fid로 찾은 기기가 회원에 바인딩돼 있으면 거부한다")
    void rejects_member_bound_device_found_by_fid() {
        resolver = new GuestDeviceResolver(memberDeviceRepository);
        MemberDevice bound = MemberDevice.builder()
                .fid("fid-bound")
                .memberDeviceType(MemberDeviceType.ANDROID)
                .member(org.mockito.Mockito.mock(Member.class))
                .build();
        given(memberDeviceRepository.findByFid("fid-bound")).willReturn(Optional.of(bound));

        assertThatThrownBy(() -> resolver.resolve("fid-bound", null))
                .isInstanceOf(UnregisteredDeviceException.class);
    }
}
