package com.dongsoop.dongsoop.restaurant.exception;

import com.dongsoop.dongsoop.common.exception.CustomException;
import org.springframework.http.HttpStatus;

public class RestaurantAlreadyExistsException extends CustomException {

    public RestaurantAlreadyExistsException(String externalMapId) {
        super("이미 등록된 식당입니다. ID: " + externalMapId, HttpStatus.CONFLICT);
    }
}