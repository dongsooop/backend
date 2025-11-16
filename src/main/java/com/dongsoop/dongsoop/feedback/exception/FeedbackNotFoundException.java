package com.dongsoop.dongsoop.feedback.exception;

import com.dongsoop.dongsoop.common.exception.CustomException;
import org.springframework.http.HttpStatus;

public class FeedbackNotFoundException extends CustomException {

    public FeedbackNotFoundException() {
        super("피드백을 찾을 수 없습니다.", HttpStatus.NOT_FOUND);
    }
}
