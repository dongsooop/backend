package com.dongsoop.dongsoop.eclass.dto;

/**
 * 이클래스 과제 상세 페이지 주소. 게시판 경로와 무관하게 코스모듈 번호만으로 열린다.
 */
public final class EclassAssignmentLink {

    private static final String VIEW_PATH = "/mod/assign/view.php?id=";

    private EclassAssignmentLink() {
    }

    public static String of(String baseUrl, Long courseModuleId) {
        return baseUrl + VIEW_PATH + courseModuleId;
    }
}
