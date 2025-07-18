package com.dongsoop.dongsoop.chat.exception;

import com.dongsoop.dongsoop.common.exception.CustomException;
import org.springframework.http.HttpStatus;

public class KickedUserInviteException extends CustomException {
  public KickedUserInviteException(Long userId) {
    super("사용자 " + userId + "는 강퇴된 상태로 초대할 수 없습니다.", HttpStatus.BAD_REQUEST);
  }
}
