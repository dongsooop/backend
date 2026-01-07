package com.dongsoop.dongsoop.memberdevice.dto;

import com.dongsoop.dongsoop.notification.constant.NotificationType;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.Collection;

public record MemberDeviceFindCondition(

        @NotEmpty
        Collection<Long> memberIds,

        @NotNull
        NotificationType notificationType
) {
}
