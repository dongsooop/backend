package com.dongsoop.dongsoop.department;

import lombok.Getter;

@Getter
public enum DepartmentType {

    // 미소속
    UNAFFILIATED("미소속", "/dmu/4904/subview.do"),

    // 컴퓨터공학부
    COMPUTER_SOFTWARE("컴퓨터소프트웨어공학과", "/dmu/4580/subview.do"),
    ARTIFICIAL_INTELLIGENCE_SOFTWARE("인공지능소프트웨어학과", "/dmu/4593/subview.do"),
    WEB_APPLICATION_SOFTWARE("웹응용소프트웨어공학과", "/dmu/4568/subview.do"),//

    // 기계공학부
    MECHANICAL_ENGINEERING("기계공학과", "/dmu/4461/subview.do"),
    MECHANICAL_DESIGN_ENGINEERING("기계설계공학과", "/dmu/4474/subview.do"),

    // 로봇자동화공학부
    AUTOMATION_ENGINEERING("자동화공학과", "/dmu/4487/subview.do"),
    ROBOTICS_SOFTWARE("로봇소프트웨어과", "/dmu/4502/subview.do"),

    // 전기전자통신공학부
    ELECTRICAL_ENGINEERING("전기공학과", "/dmu/4518/subview.do"),
    SEMICONDUCTOR_ELECTRONICS_ENGINEERING("반도체전자공학과", "/dmu/4530/subview.do"),
    INFORMATION_AND_COMMUNICATION_ENGINEERING("정보통신공학과", "/dmu/4543/subview.do"),
    FIRE_SAFETY_MANAGEMENT("소방안전관리과", "/dmu/4557/subview.do"),

    // 생활환경공학부
    CHEMICAL_AND_BIOLOGICAL_ENGINEERING("생명화학공학과", "/dmu/4605/subview.do"),
    BIO_CONVERGENCE_ENGINEERING("바이오융합공학과", "/dmu/4617/subview.do"),
    ARCHITECTURE("건축과", "/dmu/4629/subview.do"),
    INTERIOR_ARCHITECTURE_DESIGN("실내건축디자인과", "/dmu/4643/subview.do"),
    VISUAL_DESIGN("시각디자인과", "/dmu/4654/subview.do"),
    AR_VR_CONTENT_DESIGN("AR·VR콘텐츠디자인과", "/dmu/4666/subview.do"),

    // 경영학부
    BUSINESS_ADMINISTRATION("경영학과", "/dmu/4677/subview.do"),
    TAX_ACCOUNTING("세무회계학과", "/dmu/4687/subview.do"),
    DISTRIBUTION_MARKETING("유통마케팅학과", "/dmu/4697/subview.do"),
    HOTEL_AND_TOURISM("호텔관광학과", "/dmu/4708/subview.do"),
    MANAGEMENT_INFORMATION_SYSTEMS("경영정보학과", "/dmu/4719/subview.do"),
    BIG_DATA_MANAGEMENT("빅데이터경영과", "/dmu/4729/subview.do"),

    // 자유전공학과
    LIBERAL_ARTS("자유전공학과", "/dmu/4739/subview.do"),

    // 교양과
    GENERAL_EDUCATION("교양과", "/dmu/4747/subview.do")
    ;

    private final String name;
    private final String noticeUrl;

    DepartmentType(String name, String noticeUrl) {
        this.name = name;
        this.noticeUrl = noticeUrl;
    }

}
