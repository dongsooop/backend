package com.dongsoop.dongsoop.restaurant.exception;

import com.dongsoop.dongsoop.common.exception.CustomException;
import org.springframework.http.HttpStatus;

public class RestaurantNotFoundException extends CustomException {

    public RestaurantNotFoundException() {
        super("존재하지 않는 식당입니다.", HttpStatus.NOT_FOUND);
    }
}