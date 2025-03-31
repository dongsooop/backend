package com.dongsoop.dongsoop.notice.entity;

import jakarta.persistence.Column;
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

    @Column(length = 20)
    private String writer;

    @Column(length = 30)
    private String title;

    @Column(length = 50)
    private String link;

    private LocalDate createdAt;
}
