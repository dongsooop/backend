package com.dongsoop.dongsoop.eclass.notification;

import com.dongsoop.dongsoop.eclass.entity.EclassAssignment;
import com.dongsoop.dongsoop.eclass.entity.EclassLink;

public interface EclassNotification {

    /**
     * @return 실제로 푸시를 보냈으면 {@code true}. 기기 토큰이 없거나 알림이 꺼져 있어 건너뛰면 {@code false}
     */
    boolean sendReminder(EclassAssignment assignment, int daysBefore);

    /**
     * 마감이 앞당겨졌을 때 알린다. 미뤄진 마감은 리마인드가 새 일정으로 다시 나가므로 따로 알리지 않는다.
     */
    void sendDueDateChanged(EclassLink link, EclassAssignment assignment);

    /**
     * 앱이 보관한 계정으로 토큰을 재발급하도록 지시하는 무음 푸시. 사용자에게는 아무것도 보이지 않는다.
     */
    void sendRelinkSilent(EclassLink link);

    void sendExpiredNotice(EclassLink link);
}
