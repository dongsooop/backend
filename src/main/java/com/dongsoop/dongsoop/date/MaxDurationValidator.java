package com.dongsoop.dongsoop.date;

import java.time.LocalDateTime;

public class MaxDurationValidator extends DurationValidator<MaxDuration> {

    public MaxDurationValidator() {
        super();
    }

    @Override
    public void initialize(MaxDuration constraintAnnotation) {
        this.value = constraintAnnotation.value();
        this.startFieldName = constraintAnnotation.start();
        this.endFieldName = constraintAnnotation.end();
    }

    @Override
    public boolean validateDuration() {
        LocalDateTime maxTime = startDateTime.plusSeconds(this.value);
        return endDateTime.isBefore(maxTime) || endDateTime.equals(maxTime);
    }
}
