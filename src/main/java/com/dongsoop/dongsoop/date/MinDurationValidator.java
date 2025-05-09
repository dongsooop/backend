package com.dongsoop.dongsoop.date;

import java.time.LocalDateTime;

public class MinDurationValidator extends DurationValidator<MinDuration> {

    public MinDurationValidator() {
        super();
    }

    @Override
    public void initialize(MinDuration constraintAnnotation) {
        this.value = constraintAnnotation.value();
        this.startFieldName = constraintAnnotation.start();
        this.endFieldName = constraintAnnotation.end();
    }

    @Override
    public boolean validateDuration() {
        LocalDateTime minTime = endDateTime.minusSeconds(this.value);
        return startDateTime.isBefore(minTime) || startDateTime.equals(minTime);
    }
}
