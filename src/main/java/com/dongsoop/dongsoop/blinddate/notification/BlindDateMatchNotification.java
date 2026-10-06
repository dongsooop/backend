package com.dongsoop.dongsoop.blinddate.notification;

import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.member.repository.MemberRepository;
import com.dongsoop.dongsoop.notification.constant.NotificationType;
import com.dongsoop.dongsoop.notification.entity.MemberNotification;
import com.dongsoop.dongsoop.notification.service.NotificationSaveService;
import com.dongsoop.dongsoop.notification.service.NotificationSendService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 선택 화면을 떠난 회원도 알림함에서 성공한 채팅방을 확인할 수 있게 한다. */
@Component
@RequiredArgsConstructor
public class BlindDateMatchNotification {

    private final MemberRepository memberRepository;
    private final NotificationSaveService notificationSaveService;
    private final NotificationSendService notificationSendService;

    public void send(Long memberId, String roomId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalStateException("Matched member not found: " + memberId));
        // 별도 저장 서비스의 트랜잭션이 먼저 커밋된다. 푸시 실패가 저장된 알림을 롤백하지 않는다.
        MemberNotification notification = notificationSaveService.save(member,
                "과팅 매칭이 성사됐어요!", "서로 마음이 통한 상대와 채팅을 시작해 보세요.",
                NotificationType.CHAT, roomId);
        // 기존 채팅 이동 계약과 CHAT 알림 설정을 그대로 사용한다.
        notificationSendService.sendAll(List.of(notification), NotificationType.CHAT);
    }
}
