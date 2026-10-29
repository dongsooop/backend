package com.dongsoop.dongsoop.report.entity;

import com.dongsoop.dongsoop.chat.entity.ChatRoomType;

/** 메시지 신고 시점의 방 종류. 일반 채팅은 채팅 도메인의 {@link ChatRoomType}을 그대로 쓰고 과팅만 더한다. */
public enum ReportChatRoomType {
  ONE_TO_ONE,
  GROUP,
  CONTACT,
  BLINDDATE;

  public static ReportChatRoomType from(ChatRoomType chatRoomType) {
    return switch (chatRoomType) {
      case ONE_TO_ONE -> ONE_TO_ONE;
      case GROUP -> GROUP;
      case CONTACT -> CONTACT;
    };
  }
}
