package com.dongsoop.dongsoop.oauth.provider;

import com.dongsoop.dongsoop.oauth.dto.AppleJwk;
import java.util.Map;

public interface AppleJwkProvider {

    Map<String, AppleJwk> getAppleJwkMap();

    boolean evictAppleJwkCache();
}
