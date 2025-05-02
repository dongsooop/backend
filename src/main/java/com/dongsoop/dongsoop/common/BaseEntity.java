package com.dongsoop.dongsoop.common;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class BaseEntity {

    @Column(name = "is_deleted", nullable = false, columnDefinition = "boolean default false")
    private boolean isDeleted = false;

    public void setDeleted(boolean isDeleted) {
        this.isDeleted = isDeleted;
    }
}
