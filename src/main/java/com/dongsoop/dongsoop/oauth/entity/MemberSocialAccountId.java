package com.dongsoop.dongsoop.oauth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Embeddable
@NoArgsConstructor
@AllArgsConstructor
public class MemberSocialAccountId {

    @Column(name = "provider_id")
    private String providerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider_type")
    private OAuthProviderType providerType;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MemberSocialAccountId that = (MemberSocialAccountId) o;
        return Objects.equals(this.providerId, that.providerId)
                && Objects.equals(this.providerType, that.providerType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.providerId, this.providerType);
    }
}
