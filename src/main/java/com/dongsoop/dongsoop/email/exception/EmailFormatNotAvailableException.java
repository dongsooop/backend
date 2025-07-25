package com.dongsoop.dongsoop.email.exception;

import org.springframework.core.MethodParameter;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;

public class EmailFormatNotAvailableException extends MethodArgumentNotValidException {
    
    public EmailFormatNotAvailableException(MethodParameter parameter,
                                            BindingResult bindingResult) {
        super(parameter, bindingResult);
    }
}
