package com.dongsoop.dongsoop.eclass.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class EclassAssignmentSaveRequest {

    @NotNull(message = "과제 목록이 필요합니다.")
    @Valid
    private List<EclassAssignmentItem> assignments;
}
