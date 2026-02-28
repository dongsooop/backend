package com.dongsoop.dongsoop.memberdevice.service;

import com.dongsoop.dongsoop.memberdevice.dto.MemberDeviceFindCondition;
import com.dongsoop.dongsoop.memberdevice.dto.MemberDeviceResponse;
import com.dongsoop.dongsoop.memberdevice.entity.MemberDeviceType;
import java.util.List;
import java.util.Map;

public interface MemberDeviceService {

    void registerDevice(String deviceToken, MemberDeviceType deviceType);

    void bindDeviceWithMemberId(Long memberId, String deviceToken);

    List<String> getDeviceByMemberId(Long memberId);

    Map<Long, List<String>> getDeviceByMember(MemberDeviceFindCondition condition);

    void deleteByToken(String deviceToken);

    /**
     * FCM 토큰으로 기기와 회원 간의 바인딩을 해제한다.
     *
     * <p>기기의 member 참조를 {@code null}로 설정하여 익명 상태로 전환한다.
     * 알림 설정 등 기기에 연관된 데이터는 유지된다.
     *
     * @param deviceToken 바인딩을 해제할 기기의 FCM 토큰
     */
    void unbindByToken(String deviceToken);

    /**
     * 회원의 등록 기기 목록을 조회한다.
     *
     * @param memberId            조회할 회원의 ID
     * @param currentDeviceToken  현재 요청을 보낸 기기의 FCM 토큰 (null 허용)
     * @return 기기 ID, 타입, 현재 기기 여부를 담은 응답 목록
     */
    List<MemberDeviceResponse> getDeviceList(Long memberId, String currentDeviceToken);

    /**
     * 요청한 회원이 소유한 기기의 FCM 토큰을 반환한다.
     *
     * <p>기기가 존재하지 않거나 해당 회원의 기기가 아닌 경우 예외를 던진다.
     *
     * @param memberId 소유권을 확인할 회원의 ID
     * @param deviceId 조회할 기기의 ID
     * @return 해당 기기의 FCM 토큰
     * @throws com.dongsoop.dongsoop.memberdevice.exception.UnregisteredDeviceException     기기가 존재하지 않는 경우
     * @throws com.dongsoop.dongsoop.memberdevice.exception.UnauthorizedDeviceAccessException 해당 회원의 기기가 아닌 경우
     */
    String getDeviceTokenIfOwned(Long memberId, Long deviceId);

    /**
     * 기기와 회원 간의 바인딩을 해제한다.
     *
     * <p>기기의 member 참조를 {@code null}로 설정하여 익명 상태로 전환한다.
     *
     * @param deviceId 바인딩을 해제할 기기의 ID
     */
    void unbindDevice(Long deviceId);
}
