package com.dongsoop.dongsoop.common;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@MappedSuperclass
@SuperBuilder
@NoArgsConstructor
public abstract class BaseEntity {

    @Setter
    @Column(name = "is_deleted", nullable = false, columnDefinition = "boolean default false")
    private boolean isDeleted = false;
}
