package com.dongsoop.dongsoop.blinddate.exception;

import lombok.Getter;

@Getter
public class InvalidBlindDateChoiceException extends RuntimeException {
    private final int status;
    private final String code;

    public InvalidBlindDateChoiceException(int status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }
}
