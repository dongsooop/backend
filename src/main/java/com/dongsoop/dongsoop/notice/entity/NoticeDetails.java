package com.dongsoop.dongsoop.notice.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@AllArgsConstructor
@NoArgsConstructor
public class NoticeDetails {

    @Id
    @Getter
    private Long id;

    @Getter
    private String writer;

    @Getter
    private String title;

    @Getter
    private String link;

    private LocalDate createdAt;
}
