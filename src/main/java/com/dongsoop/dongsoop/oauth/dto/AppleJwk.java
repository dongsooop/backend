package com.dongsoop.dongsoop.oauth.dto;

public record AppleJwk(
        String kty,
        String kid,
        String use,
        String alg,
        String n,
        String e
) {
}
