package com.dongsoop.dongsoop.jwt.dto;

import java.util.Collection;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;

@AllArgsConstructor
@Getter
public class AuthenticationInformationByToken {

    private Long id;
    private Collection<? extends GrantedAuthority> role;
}
