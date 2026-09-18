package com.dongsoop.dongsoop.monitoring.constant;

import java.util.Map;

/** 요청 경로 첫 세그먼트(feature 키) → 리포트에 보여줄 한글 이름. 표에 없으면 키를 그대로 쓴다. */
public final class FeatureLabel {

    private static final Map<String, String> LABELS = Map.ofEntries(
            Map.entry("system", "자동 호출(앱 시작 등)"),
            Map.entry("blinddate", "소개팅"),
            Map.entry("chat", "채팅"),
            Map.entry("device", "기기"),
            Map.entry("eclass", "이클래스"),
            Map.entry("feedback", "피드백"),
            Map.entry("home", "홈"),
            Map.entry("mail-verify", "메일 인증"),
            Map.entry("marketplace-board", "장터"),
            Map.entry("marketplace-contact", "장터 문의"),
            Map.entry("meal", "학식"),
            Map.entry("member", "회원"),
            Map.entry("member-block", "회원 차단"),
            Map.entry("mypage", "마이페이지"),
            Map.entry("notice", "공지"),
            Map.entry("notification-settings", "알림 설정"),
            Map.entry("notifications", "알림함"),
            Map.entry("oauth2", "소셜 로그인"),
            Map.entry("project-apply", "프로젝트 지원"),
            Map.entry("project-board", "프로젝트 모집"),
            Map.entry("reports", "신고"),
            Map.entry("restaurants", "맛집"),
            Map.entry("schedule", "일정"),
            Map.entry("search", "검색"),
            Map.entry("study-apply", "스터디 지원"),
            Map.entry("study-board", "스터디 모집"),
            Map.entry("subscribe-department", "학과 구독"),
            Map.entry("timetable", "시간표"),
            Map.entry("token", "토큰 갱신"),
            Map.entry("tutoring-apply", "튜터링 지원"),
            Map.entry("tutoring-board", "튜터링 모집"),
            Map.entry("web-app-check", "웹 앱체크")
    );

    private FeatureLabel() {
    }

    public static String of(String feature) {
        return LABELS.getOrDefault(feature, feature);
    }
}
