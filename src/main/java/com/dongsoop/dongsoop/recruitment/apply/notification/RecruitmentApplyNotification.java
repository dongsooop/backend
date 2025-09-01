package com.dongsoop.dongsoop.recruitment.apply.notification;

import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.member.repository.MemberRepository;
import com.dongsoop.dongsoop.notification.constant.NotificationType;
import com.dongsoop.dongsoop.notification.entity.MemberNotification;
import com.dongsoop.dongsoop.notification.service.NotificationSaveService;
import com.dongsoop.dongsoop.notification.service.NotificationSendService;
import org.springframework.scheduling.annotation.Async;

public abstract class RecruitmentApplyNotification {

    private static final Integer TITLE_TRUNCATION_LENGTH = 8;

    private final NotificationSaveService notificationSaveService;
    private final NotificationSendService notificationSendService;
    private final MemberRepository memberRepository;

    public RecruitmentApplyNotification(NotificationSaveService notificationSaveService,
                                        NotificationSendService notificationSendService,
                                        MemberRepository memberRepository) {
        this.notificationSaveService = notificationSaveService;
        this.notificationSendService = notificationSendService;
        this.memberRepository = memberRepository;
    }

    protected abstract NotificationType getApplyNotificationType();

    protected abstract NotificationType getOutcomeNotificationType();

    @Async
    public void sendApplyNotification(Long boardId, String boardTitle, Long ownerId) {
        Member owner = memberRepository.getReferenceById(ownerId);

        String processBoardTitle = processBoardTitle(boardTitle);
        String title = "모집 지원자 알림";
        String body = "[" + processBoardTitle + "] 새로운 지원자가 있습니다. 확인해보세요!";
        NotificationType type = getApplyNotificationType();

        String value = String.valueOf(boardId);

        // 저장
        MemberNotification memberNotification = notificationSaveService.save(owner, title, body, type, value);

        // 알림 전송
        notificationSendService.send(memberNotification);
    }

    @Async
    public void sendOutcomeNotification(Long boardId, String boardTitle, Long applierId) {
        Member applier = memberRepository.getReferenceById(applierId);

        String processBoardTitle = processBoardTitle(boardTitle);
        String title = "모집 결과 알림";
        String body = "[" + processBoardTitle + "] 모집 지원 결과가 등록되었습니다. 행운을 빌어요!";
        NotificationType type = getOutcomeNotificationType();

        // 저장
        MemberNotification memberNotification = notificationSaveService.save(applier, title, body, type,
                String.valueOf(boardId));

        // 알림 전송
        notificationSendService.send(memberNotification);
    }

    private String processBoardTitle(String boardTitle) {
        if (boardTitle.length() <= TITLE_TRUNCATION_LENGTH) {
            return boardTitle;
        }

        return boardTitle.substring(0, TITLE_TRUNCATION_LENGTH) + "...";
    }
}
