package com.dongsoop.dongsoop.eclass.service;

import com.dongsoop.dongsoop.eclass.entity.EclassLink;

public interface EclassSyncService {

    /**
     * 연동 1건의 과제를 이클래스에서 가져와 반영한다. 외부 호출을 포함하므로 트랜잭션 밖에서 호출한다.
     */
    SyncOutcome syncLink(EclassLink link);

    /**
     * checkAllSubmissions 가 true 면 수집 창 안의 모든 과제에 제출 여부를 묻는다.
     * 연동 직후와 사용자의 수동 새로고침처럼 지금 정확해야 하는 경우에 쓴다.
     */
    SyncOutcome syncLink(EclassLink link, boolean checkAllSubmissions);

    void syncAll();

    enum SyncOutcome {
        SYNCED,
        TOKEN_EXPIRED,
        FAILED
    }
}
