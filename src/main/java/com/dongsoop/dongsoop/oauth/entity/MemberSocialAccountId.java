package com.dongsoop.dongsoop.oauth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MemberSocialAccountId that = (MemberSocialAccountId) o;
        return this.providerId.equals(that.providerId)
                && this.providerType.equals(that.providerType);
    }

    public int hashCode() {
        int result = 17;

        result = 31 * result + (providerId != null ? providerId.hashCode() : 0);
        result = 31 * result + (providerType != null ? providerType.hashCode() : 0);

        return result;
    }
}
