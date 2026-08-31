package com.dongsoop.dongsoop.eclass.notification;

import com.dongsoop.dongsoop.eclass.entity.EclassAssignment;
import com.dongsoop.dongsoop.eclass.entity.EclassLink;

public interface EclassNotification {

    void sendReminder(EclassAssignment assignment, int daysBefore);

    /**
     * 앱이 보관한 계정으로 토큰을 재발급하도록 지시하는 무음 푸시. 사용자에게는 아무것도 보이지 않는다.
     */
    void sendRelinkSilent(EclassLink link);

    void sendExpiredNotice(EclassLink link);
}
