package com.dongsoop.dongsoop.report.dto;

import com.dongsoop.dongsoop.report.entity.ChatMessageSnapshot;
import java.util.List;

public record ReportContextResponse(String messageId, String messageContent, List<ChatMessageSnapshot> before,
                                    List<ChatMessageSnapshot> after, AfterSource afterSource) {

    public enum AfterSource {
        LIVE,        // 조회 시점에 원문(채팅 저장소·과팅 세션 메모리)에서 가져옴
        SAVED,       // 과팅 세션 종료 때 신고에 저장해 둔 것
        UNAVAILABLE  // 원문도 저장본도 없음
    }
}
