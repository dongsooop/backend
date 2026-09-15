package com.dongsoop.dongsoop.eclass.service;

import com.dongsoop.dongsoop.eclass.entity.EclassLink;

public interface EclassSyncService {

    /**
     * 연동 1건의 과제를 이클래스에서 가져와 반영한다. 외부 호출을 포함하므로 트랜잭션 밖에서 호출한다.
     */
    SyncOutcome syncLink(EclassLink link);

    void syncAll();

    enum SyncOutcome {
        SYNCED,
        TOKEN_EXPIRED,
        FAILED
    }
}
