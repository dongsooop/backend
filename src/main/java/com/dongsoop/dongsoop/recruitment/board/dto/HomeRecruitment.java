package com.dongsoop.dongsoop.recruitment.board.dto;

import com.dongsoop.dongsoop.recruitment.RecruitmentType;
import lombok.Getter;

@Getter
public class HomeRecruitment {

    private final Long id;
    private final Long volunteer;
    private final String title;
    private final String content;
    private final String tags;
    private final RecruitmentType type;

    public HomeRecruitment(Long id, Long volunteer, String title, String content, String tags, String type) {
        this.id = id;
        this.volunteer = volunteer;
        this.title = title;
        this.content = content;
        this.tags = tags;
        this.type = RecruitmentType.valueOf(type);
    }
}
