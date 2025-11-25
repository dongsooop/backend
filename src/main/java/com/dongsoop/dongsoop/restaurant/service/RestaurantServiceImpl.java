package com.dongsoop.dongsoop.restaurant.service;

import com.dongsoop.dongsoop.common.exception.authentication.NotAuthenticationException;
import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.member.repository.MemberRepository;
import com.dongsoop.dongsoop.member.service.MemberService;
import com.dongsoop.dongsoop.restaurant.dto.RestaurantOverview;
import com.dongsoop.dongsoop.restaurant.dto.RestaurantRegisterRequest;
import com.dongsoop.dongsoop.restaurant.entity.Restaurant;
import com.dongsoop.dongsoop.restaurant.entity.RestaurantLike;
import com.dongsoop.dongsoop.restaurant.entity.RestaurantReport;
import com.dongsoop.dongsoop.restaurant.entity.RestaurantReportReason;
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
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RestaurantServiceImpl implements RestaurantService {

    private final RestaurantRepository restaurantRepository;
    private final RestaurantLikeRepository restaurantLikeRepository;
    private final MemberRepository memberRepository;
    private final RestaurantMapper restaurantMapper;
    private final RestaurantReportRepository restaurantReportRepository;
    private final MemberService memberService;

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
    public List<RestaurantOverview> getNearbyRestaurants(Pageable pageable) {
        Long memberId = null;
        try {
            memberId = memberService.getMemberIdByAuthentication();
        } catch (NotAuthenticationException e) {

        }
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

        if (isAdding && !isCurrentlyLiked) {
            addLike(key);
            return;
        }

        if (!isAdding && isCurrentlyLiked) {
            removeLike(key);
        }
    }

    private void addLike(RestaurantLike.RestaurantLikeKey key) {
        RestaurantLike restaurantLike = RestaurantLike.builder()
                .id(key)
                .build();
        restaurantLikeRepository.save(restaurantLike);
    }

    private void removeLike(RestaurantLike.RestaurantLikeKey key) {
        restaurantLikeRepository.deleteById(key);
    }

    private Restaurant findRestaurantById(Long restaurantId) {
        return restaurantRepository.findById(restaurantId)
                .orElseThrow(RestaurantNotFoundException::new);
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