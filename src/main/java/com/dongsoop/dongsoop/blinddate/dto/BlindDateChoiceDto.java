package com.dongsoop.dongsoop.blinddate.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 과팅 선택 DTO (사랑의 작대기) (choicerId는 Principal에서 추출)
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class BlindDateChoiceDto {
    private Long targetId;
}
