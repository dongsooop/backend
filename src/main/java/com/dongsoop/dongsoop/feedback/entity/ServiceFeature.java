package com.dongsoop.dongsoop.feedback.entity;

public enum ServiceFeature {

    NOTICE_ALERT("교내 공지 알림 서비스"),
    MEAL_INFORMATION("학식 정보 확인 서비스"),
    ACADEMIC_SCHEDULE("학사 일정 확인 및 개인 일정 관리"),
    TIMETABLE_AUTO_MANAGE("시간표 자동 입력 및 관리"),
    TEAM_RECRUITMENT("팀원 모집(스터디/튜터링/프로젝트)"),
    MARKETPLACE("장터(교재 등 중고 거래)"),
    CHATBOT_CAMPUS_INFO("챗봇을 통한 교내 정보 확인");

    private final String description;

    ServiceFeature(String description) {
        this.description = description;
    }
}
