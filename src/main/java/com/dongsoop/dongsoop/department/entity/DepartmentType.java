package com.dongsoop.dongsoop.department.entity;

import java.util.HashMap;
import java.util.Map;
import lombok.Getter;

public enum DepartmentType {

    // 전체
    DEPT_1001("UNAFFILIATED", "동양미래대학"),

    // 컴퓨터공학부
    DEPT_2001("COMPUTER_SOFTWARE", "컴퓨터소프트웨어공학과"),
    DEPT_2002("ARTIFICIAL_INTELLIGENCE_SOFTWARE", "인공지능소프트웨어학과"),
    DEPT_2003("WEB_APPLICATION_SOFTWARE", "웹응용소프트웨어공학과"),

    // 기계공학부
    DEPT_3001("MECHANICAL_ENGINEERING", "기계공학과"),
    DEPT_3002("MECHANICAL_DESIGN_ENGINEERING", "기계설계공학과"),

    // 로봇자동화공학부
    DEPT_4001("AUTOMATION_ENGINEERING", "자동화공학과"),
    DEPT_4002("ROBOTICS_SOFTWARE", "로봇소프트웨어과"),

    // 전기전자통신공학부
    DEPT_5001("ELECTRICAL_ENGINEERING", "전기공학과"),
    DEPT_5002("SEMICONDUCTOR_ELECTRONICS_ENGINEERING", "반도체전자공학과"),
    DEPT_5003("INFORMATION_AND_COMMUNICATION_ENGINEERING", "정보통신공학과"),
    DEPT_5004("FIRE_SAFETY_MANAGEMENT", "소방안전관리과"),

    // 생활환경공학부
    DEPT_6001("CHEMICAL_AND_BIOLOGICAL_ENGINEERING", "생명화학공학과"),
    DEPT_6002("BIO_CONVERGENCE_ENGINEERING", "바이오융합공학과"),
    DEPT_6003("ARCHITECTURE", "건축과"),
    DEPT_6004("INTERIOR_ARCHITECTURE_DESIGN", "실내건축디자인과"),
    DEPT_6005("VISUAL_DESIGN", "시각디자인과"),
    DEPT_6006("AR_VR_CONTENT_DESIGN", "AR·VR콘텐츠디자인과"),

    // 경영학부
    DEPT_7001("BUSINESS_ADMINISTRATION", "경영학과"),
    DEPT_7002("TAX_ACCOUNTING", "세무회계학과"),
    DEPT_7003("DISTRIBUTION_MARKETING", "유통마케팅학과"),
    DEPT_7004("HOTEL_AND_TOURISM", "호텔관광학과"),
    DEPT_7005("MANAGEMENT_INFORMATION_SYSTEMS", "경영정보학과"),
    DEPT_7006("BIG_DATA_MANAGEMENT", "빅데이터경영과"),

    // 자유전공학과
    DEPT_8001("LIBERAL_ARTS", "자유전공학과"),

    // 교양과
    DEPT_9001("GENERAL_EDUCATION", "교양과");

    private static final Map<String, DepartmentType> idMap = new HashMap<>();

    static {
        for (DepartmentType type : DepartmentType.values()) {
            idMap.put(type.getId(), type);
        }
    }

    @Getter
    private final String id;

    @Getter
    private final String name;

    DepartmentType(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public static DepartmentType from(String id) {
        return idMap.get(id);
    }

    public boolean isAllDepartment() {
        return this == DEPT_1001;
    }
}
