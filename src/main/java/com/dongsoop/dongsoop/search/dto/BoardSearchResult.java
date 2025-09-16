package com.dongsoop.dongsoop.search.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class BoardSearchResult {
    private String id;
    private String title;
    private String content;
    private String boardType;
    private Long boardId;
    private String authorName;
    private String createdAt;

    private LocalDateTime recruitmentStartAt;
    private LocalDateTime recruitmentEndAt;
    private String marketplaceStatus;
    private String marketplaceType;
    private String noticeUrl;
    private String tags;
    private Long price;
}
