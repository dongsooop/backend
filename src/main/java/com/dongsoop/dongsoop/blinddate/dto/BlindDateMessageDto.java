package com.dongsoop.dongsoop.blinddate.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 과팅 메시지 전송 DTO (memberId는 Principal에서 추출)
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class BlindDateMessageDto {
    private String message;
}
