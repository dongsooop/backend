package com.dongsoop.dongsoop.feedback.dto;

import com.dongsoop.dongsoop.feedback.entity.ServiceFeature;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.hibernate.validator.constraints.Length;

public record FeedbackCreate(

        @Size(min = 1, max = 3, message = "서비스 기능은 최소 1개에서 최대 3개까지 선택할 수 있습니다.")
        List<ServiceFeature> feature,

        @Length(min = 1, max = 150, message = "개선 요청사항은 비워둘 수 없으며 최대 150자까지 입력할 수 있습니다.")
        String improvementSuggestions,

        @Length(min = 1, max = 150, message = "추가 기능은 비워둘 수 없으며 최대 150자까지 입력할 수 있습니다.")
        String featureRequests
) {
}
