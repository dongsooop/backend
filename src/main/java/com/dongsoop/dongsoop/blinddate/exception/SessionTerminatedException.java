package com.dongsoop.dongsoop.blinddate.exception;

public class SessionTerminatedException extends RuntimeException {

    public SessionTerminatedException() {
        super("세션이 종료된 상태입니다.");
    }
}
