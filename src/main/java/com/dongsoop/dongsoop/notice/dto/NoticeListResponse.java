package com.dongsoop.dongsoop.notice.dto;

import java.time.LocalDate;

public interface NoticeListResponse {
    
    Long getId();

    String getWriter();

    String getTitle();

    String getLink();

    LocalDate getCreatedAt();
}
