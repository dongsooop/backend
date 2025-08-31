package com.dongsoop.dongsoop.notification.dto;

import com.google.firebase.messaging.MulticastMessage;
import java.util.List;

public record MulticastMessageWithTokens(

        List<String> tokens,
        MulticastMessage messages
) {
}
