package com.dongsoop.dongsoop.blinddate.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 블라인드 데이트 세션 정보
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlindDateSession {
    
    private String sessionId;
    private Long memberId;
    private String gender;
    private String department;
    private String matchedWith; // 매칭된 상대방의 sessionId
    private SessionStatus status;
    private Long createdAt;
    
    public void updateStatus(SessionStatus status) {
        this.status = status;
    }
    
    public void setMatchedWith(String matchedSessionId) {
        this.matchedWith = matchedSessionId;
        this.status = SessionStatus.MATCHED;
    }
    
    public void clearMatch() {
        this.matchedWith = null;
        this.status = SessionStatus.WAITING;
    }
    
    public boolean isMatched() {
        return this.matchedWith != null;
    }
}
