package com.dongsoop.dongsoop.department.entity;

import java.util.HashMap;
import java.util.Map;
import lombok.Getter;

public enum DepartmentType {

    // 전체
    DEPT_1001("UNAFFILIATED"),

    // 컴퓨터공학부
    DEPT_2001("COMPUTER_SOFTWARE"),
    DEPT_2002("ARTIFICIAL_INTELLIGENCE_SOFTWARE"),
    DEPT_2003("WEB_APPLICATION_SOFTWARE"),

    // 기계공학부
    DEPT_3001("MECHANICAL_ENGINEERING"),
    DEPT_3002("MECHANICAL_DESIGN_ENGINEERING"),

    // 로봇자동화공학부
    DEPT_4001("AUTOMATION_ENGINEERING"),
    DEPT_4002("ROBOTICS_SOFTWARE"),

    // 전기전자통신공학부
    DEPT_5001("ELECTRICAL_ENGINEERING"),
    DEPT_5002("SEMICONDUCTOR_ELECTRONICS_ENGINEERING"),
    DEPT_5003("INFORMATION_AND_COMMUNICATION_ENGINEERING"),
    DEPT_5004("FIRE_SAFETY_MANAGEMENT"),

    // 생활환경공학부
    DEPT_6001("CHEMICAL_AND_BIOLOGICAL_ENGINEERING"),
    DEPT_6002("BIO_CONVERGENCE_ENGINEERING"),
    DEPT_6003("ARCHITECTURE"),
    DEPT_6004("INTERIOR_ARCHITECTURE_DESIGN"),
    DEPT_6005("VISUAL_DESIGN"),
    DEPT_6006("AR_VR_CONTENT_DESIGN"),

    // 경영학부
    DEPT_7001("BUSINESS_ADMINISTRATION"),
    DEPT_7002("TAX_ACCOUNTING"),
    DEPT_7003("DISTRIBUTION_MARKETING"),
    DEPT_7004("HOTEL_AND_TOURISM"),
    DEPT_7005("MANAGEMENT_INFORMATION_SYSTEMS"),
    DEPT_7006("BIG_DATA_MANAGEMENT"),

    // 자유전공학과
    DEPT_8001("LIBERAL_ARTS"),

    // 교양과
    DEPT_9001("GENERAL_EDUCATION");

    @Getter
    private final String id;

    private static final Map<String, DepartmentType> idMap = new HashMap<>();

    static {
        for (DepartmentType type : DepartmentType.values()) {
            idMap.put(type.getId(), type);
        }
    }

    DepartmentType(String id) {
        this.id = id;
    }

    public static DepartmentType from(String id) {
        return idMap.get(id);
    }
}
