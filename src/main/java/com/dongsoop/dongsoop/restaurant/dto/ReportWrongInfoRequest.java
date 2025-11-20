package com.dongsoop.dongsoop.restaurant.dto;

import jakarta.validation.constraints.Size;

public record ReportWrongInfoRequest(
        @Size(max = 500, message = "신고 내용은 500자 이하로 입력해주세요.")
        String description
) {
}