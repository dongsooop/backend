package com.dongsoop.dongsoop.report.entity;

import java.util.EnumSet;
import java.util.Set;

public enum ReportType {
    PROJECT_BOARD,
    STUDY_BOARD,
    MARKETPLACE_BOARD,
    TUTORING_BOARD,
    MEMBER;

    public static final Set<ReportType> BOARD_TYPES =
            EnumSet.of(PROJECT_BOARD, STUDY_BOARD, MARKETPLACE_BOARD, TUTORING_BOARD);
}
