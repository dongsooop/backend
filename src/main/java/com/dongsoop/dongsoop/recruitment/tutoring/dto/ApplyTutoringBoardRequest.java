package com.dongsoop.dongsoop.recruitment.tutoring.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ApplyTutoringBoardRequest {

    private Long boardId;

    private String introduction;

    private String motivation;
}
