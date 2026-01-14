package com.dongsoop.dongsoop.oauth.dto;

import com.dongsoop.dongsoop.role.entity.RoleType;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

public class CustomOAuth2User implements OAuth2User {

    private final OAuth2User delegate; // 기존 OAuth2User (구글, 카카오 등에서 받은 정보)

    @Getter
    private final Long memberId; // 우리 DB의 PK

    @Getter
    private final List<RoleType> roleTypeList;

    public CustomOAuth2User(OAuth2User delegate, Long memberId, List<RoleType> roleTypeList) {
        this.delegate = delegate;
        this.memberId = memberId;
        this.roleTypeList = roleTypeList;
    }

    // OAuth2User 메서드 위임
    @Override
    public Map<String, Object> getAttributes() {
        return delegate.getAttributes();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return delegate.getAuthorities(); // 혹은 role 기반으로 생성해서 반환
    }

    @Override
    public String getName() {
        return delegate.getName();
    }
}
