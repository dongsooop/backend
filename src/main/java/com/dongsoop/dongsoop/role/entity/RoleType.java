package com.dongsoop.dongsoop.role.entity;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

public enum RoleType {
    ADMIN,
    USER;

    public static final String USER_ROLE = "ROLE_USER";
    public static final String ADMIN_ROLE = "ROLE_ADMIN";
    private static final String ROLE_PREFIX = "ROLE_";

    public GrantedAuthority getAuthority() {
        return new SimpleGrantedAuthority(ROLE_PREFIX + this.name());
    }
}
