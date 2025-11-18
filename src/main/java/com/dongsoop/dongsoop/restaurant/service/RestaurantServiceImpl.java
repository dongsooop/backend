package com.dongsoop.dongsoop.restaurant.service;

import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.member.repository.MemberRepository;
import com.dongsoop.dongsoop.restaurant.dto.RestaurantOverview;
import com.dongsoop.dongsoop.restaurant.dto.RestaurantRegisterRequest;
import com.dongsoop.dongsoop.restaurant.entity.*;
import com.dongsoop.dongsoop.restaurant.exception.RestaurantAlreadyExistsException;
import com.dongsoop.dongsoop.restaurant.exception.RestaurantNotFoundException;
import com.dongsoop.dongsoop.restaurant.repository.RestaurantLikeRepository;
import com.dongsoop.dongsoop.restaurant.repository.RestaurantReportRepository;
import com.dongsoop.dongsoop.restaurant.repository.RestaurantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RestaurantServiceImpl implements RestaurantService {

    private static final double MAX_DISTANCE_KM = 1.0;
    private final RestaurantRepository restaurantRepository;
    private final RestaurantLikeRepository restaurantLikeRepository;
    private final MemberRepository memberRepository;
    private final RestaurantMapper restaurantMapper;

    private final RestaurantReportRepository restaurantReportRepository;

    @Value("${school.location.latitude}")
    private double SCHOOL_LATITUDE;
    @Value("${school.location.longitude}")
    private double SCHOOL_LONGITUDE;

    @Transactional
    @Override
    public Restaurant registerRestaurant(RestaurantRegisterRequest request) {
        if (restaurantRepository.existsByExternalMapId(request.externalMapId())) {
            throw new RestaurantAlreadyExistsException(request.externalMapId());
        }
        Restaurant restaurant = restaurantMapper.toEntity(request);
        return restaurantRepository.save(restaurant);
    }

    @Transactional(readOnly = true)
    @Override
    public List<RestaurantOverview> getNearbyRestaurants(Long memberId, Pageable pageable) {
        return restaurantRepository.findNearbyRestaurants(
                SCHOOL_LATITUDE, SCHOOL_LONGITUDE, MAX_DISTANCE_KM, memberId, pageable);
    }

    @Transactional
    @Override
    public void addLike(Long restaurantId, Long memberId) {
        if (restaurantLikeRepository.existsByRestaurantIdAndMemberId(restaurantId, memberId)) {
            return;
        }

        Restaurant restaurant = findRestaurantReferenceById(restaurantId);
        Member member = memberRepository.getReferenceById(memberId);

        RestaurantLike.RestaurantLikeKey key = RestaurantLike.RestaurantLikeKey.builder()
                .restaurant(restaurant)
                .member(member)
                .build();

        RestaurantLike restaurantLike = RestaurantLike.builder().id(key).build();
        restaurantLikeRepository.save(restaurantLike);
    }

    @Transactional
    @Override
    public void removeLike(Long restaurantId, Long memberId) {
        Restaurant restaurant = findRestaurantReferenceById(restaurantId);
        Member member = memberRepository.getReferenceById(memberId);

        RestaurantLike.RestaurantLikeKey key = RestaurantLike.RestaurantLikeKey.builder()
                .restaurant(restaurant)
                .member(member)
                .build();

        restaurantLikeRepository.deleteById(key);
    }

    private Restaurant findRestaurantReferenceById(Long restaurantId) {
        return restaurantRepository.findById(restaurantId)
                .orElseThrow(RestaurantNotFoundException::new);
    }

    @Transactional(readOnly = true)
    @Override
    public List<RestaurantOverview> getRestaurantsByStatus(RestaurantStatus status, Long memberId, Pageable pageable) {
        return restaurantRepository.findRestaurantsByStatus(status, pageable, memberId);
    }

    @Transactional
    @Override
    public void approveRestaurant(Long restaurantId) {
        Restaurant restaurant = findRestaurantReferenceById(restaurantId);
        restaurant.approve();
    }

    @Transactional
    @Override
    public void rejectRestaurant(Long restaurantId) {
        Restaurant restaurant = findRestaurantReferenceById(restaurantId);
        restaurantRepository.delete(restaurant);
    }

    @Transactional
    @Override
    public void createRestaurantReport(Long restaurantId, Long reporterId, RestaurantReportReason reason, String description) {
        Restaurant restaurant = findRestaurantReferenceById(restaurantId);
        Member reporter = memberRepository.getReferenceById(reporterId);

        RestaurantReport report = RestaurantReport.builder()
                .restaurant(restaurant)
                .reporter(reporter)
                .reason(reason)
                .description(description)
                .build();

        restaurantReportRepository.save(report);
    }
}