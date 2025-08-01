package com.dongsoop.dongsoop.common.config;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;

public class AuthErrorFilter extends Filter<ILoggingEvent> {

    @Override
    public FilterReply decide(ILoggingEvent event) {
        String message = event.getFormattedMessage();

        // "Full authentication is required" 메시지가 포함된 로그는 디스코드에서 제외
        if (message != null && message.contains("Full authentication is required to access this resource")) {
            return FilterReply.DENY;
        }

        return FilterReply.NEUTRAL;
    }
}