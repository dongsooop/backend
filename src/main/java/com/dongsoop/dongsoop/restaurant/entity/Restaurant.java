package com.dongsoop.dongsoop.restaurant.entity;

import com.dongsoop.dongsoop.common.BaseEntity;
import com.dongsoop.dongsoop.recruitment.validation.constant.RecruitmentValidationConstant;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.util.Objects;

@Entity
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLRestriction("is_deleted = false")
@SQLDelete(sql = "UPDATE restaurant SET is_deleted = true, external_map_id = external_map_id || '_' || id WHERE id = ?")
@SequenceGenerator(name = "restaurant_sequence_generator", sequenceName = "restaurant_sequence", allocationSize = 1)
public class Restaurant extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "restaurant_sequence_generator")
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, length = 512)
    private String address;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(nullable = false)
    private String placeUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private RestaurantCategory category;

    @Column(nullable = false, unique = true)
    private String externalMapId;

    @Column(length = 20)
    private String phone;

    @Column(name = "tags", length = RecruitmentValidationConstant.TAG_MAX_LENGTH, nullable = false)
    private String tags;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private RestaurantStatus status = RestaurantStatus.PENDING;

    public void approve() {
        this.status = RestaurantStatus.APPROVED;
    }

    public void reject() {
        this.status = RestaurantStatus.REJECTED;
    }

    public boolean equalsId(Restaurant that) {
        return Objects.equals(this.id, that.id);
    }
}