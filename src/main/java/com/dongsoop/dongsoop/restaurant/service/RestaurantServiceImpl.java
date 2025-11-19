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
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
public class RestaurantServiceImpl implements RestaurantService {

    private final RestaurantRepository restaurantRepository;
    private final RestaurantLikeRepository restaurantLikeRepository;
    private final MemberRepository memberRepository;
    private final RestaurantMapper restaurantMapper;
    private final RestaurantReportRepository restaurantReportRepository;

    @Transactional
    @Override
    public Restaurant registerRestaurant(RestaurantRegisterRequest request) {
        Optional.of(request.externalMapId())
                .filter(restaurantRepository::existsActiveByExternalMapId)
                .ifPresent(id -> {
                    throw new RestaurantAlreadyExistsException(id);
                });

        Restaurant restaurant = restaurantMapper.toEntity(request);
        return restaurantRepository.save(restaurant);
    }

    @Transactional(readOnly = true)
    @Override
    public List<RestaurantOverview> getNearbyRestaurants(Long memberId, Pageable pageable) {
        return restaurantRepository.findNearbyRestaurants(memberId, pageable);
    }

    @Transactional
    @Override
    public void toggleLike(Long restaurantId, Long memberId, boolean isAdding) {
        Restaurant restaurant = findRestaurantById(restaurantId);
        Member member = memberRepository.getReferenceById(memberId);

        RestaurantLike.RestaurantLikeKey key = RestaurantLike.RestaurantLikeKey.builder()
                .restaurant(restaurant)
                .member(member)
                .build();

        boolean isCurrentlyLiked = restaurantLikeRepository.existsById(key);

        Map<Boolean, Map<Boolean, Consumer<Restaurant>>> actionMap = Map.of(
                true, Map.of(
                        false, r -> {
                            RestaurantLike restaurantLike = RestaurantLike.builder().id(key).build();
                            restaurantLikeRepository.save(restaurantLike);
                            r.increaseLikeCount();
                        },
                        true, r -> {
                        }
                ),
                false, Map.of(
                        true, r -> {
                            restaurantLikeRepository.deleteById(key);
                            r.decreaseLikeCount();
                        },
                        false, r -> {
                        }
                )
        );

        actionMap.getOrDefault(isAdding, Map.of()).getOrDefault(isCurrentlyLiked, r -> {
        }).accept(restaurant);
    }

    private Restaurant findRestaurantById(Long restaurantId) {
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
        Restaurant restaurant = findRestaurantById(restaurantId);
        restaurant.approve();
    }

    @Transactional
    @Override
    public void rejectRestaurant(Long restaurantId) {
        Restaurant restaurant = findRestaurantById(restaurantId);
        restaurantRepository.delete(restaurant);
    }

    @Transactional
    @Override
    public void createRestaurantReport(Long restaurantId, Long reporterId, RestaurantReportReason reason, String description) {
        Restaurant restaurant = findRestaurantById(restaurantId);
        Member reporter = memberRepository.getReferenceById(reporterId);

        RestaurantReport report = RestaurantReport.builder()
                .restaurant(restaurant)
                .reporter(reporter)
                .reason(reason)
                .description(description)
                .build();

        restaurantReportRepository.save(report);
    }

    @Transactional(readOnly = true)
    @Override
    public boolean checkDuplicateByExternalId(String externalMapId) {
        return restaurantRepository.existsActiveByExternalMapId(externalMapId);
    }
}