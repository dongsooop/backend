package com.dongsoop.dongsoop.chat.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ChatRoomType {
    CONTACT("contact"),
    GROUP("group"),
    ONE_TO_ONE("oneToOne");

    private final String value;
}
