package com.dongsoop.dongsoop.oauth.dto;

public record OAuthLoginRequest(

        String authorizationToken,

        String deviceToken
) {
}
