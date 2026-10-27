package com.dongsoop.dongsoop.notification;

import static org.assertj.core.api.Assertions.assertThat;

import com.dongsoop.dongsoop.notification.dto.NotificationUnread;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class NotificationUnreadTest {

    @ParameterizedTest
    @CsvSource(value = {
            "null, 0",
            "-1, 0",
            "-9223372036854775808, 0",
            "0, 0",
            "42, 42",
            "2147483647, 2147483647",
            "2147483648, 2147483647",
            "9223372036854775807, 2147483647"
    }, nullValues = "null")
    void normalizesCountAndPreservesMember(Long count, int expected) {
        NotificationUnread unread = new NotificationUnread(7L, count);

        assertThat(unread.getUnreadCount()).isEqualTo(expected);
        assertThat(unread.getMemberId()).isEqualTo(7L);
    }
}
