package com.dongsoop.dongsoop.report.entity;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public enum ReportType {
  PROJECT_BOARD,
  STUDY_BOARD,
  MARKETPLACE_BOARD,
  TUTORING_BOARD,
  MEMBER,
  CHAT_MESSAGE,
  BLINDDATE_MESSAGE;

  public static final Set<ReportType> BOARD_TYPES =
      Collections.unmodifiableSet(
          EnumSet.of(PROJECT_BOARD, STUDY_BOARD, MARKETPLACE_BOARD, TUTORING_BOARD));

  public static final Set<ReportType> MESSAGE_TYPES =
      Collections.unmodifiableSet(EnumSet.of(CHAT_MESSAGE, BLINDDATE_MESSAGE));

  public boolean isMessageReport() {
    return MESSAGE_TYPES.contains(this);
  }
}
