package com.dongsoop.dongsoop.memberdevice.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class DeviceUtil {

    private DeviceUtil() {
    }

    public static Long getDeviceIdFromContext() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getDetails() instanceof Long id) {
            return id;
        }
        return null;
    }
}
