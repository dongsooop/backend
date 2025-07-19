package com.dongsoop.dongsoop.search.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
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
    
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private LocalDateTime recruitmentStartAt;
    
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private LocalDateTime recruitmentEndAt;
    
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String marketplaceStatus;
    
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String noticeUrl;
    
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String tags;
}
