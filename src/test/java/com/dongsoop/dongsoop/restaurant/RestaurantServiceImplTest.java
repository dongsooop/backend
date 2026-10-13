package com.dongsoop.dongsoop.restaurant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.dongsoop.dongsoop.common.exception.authentication.NotAuthenticationException;
import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.member.repository.MemberRepository;
import com.dongsoop.dongsoop.member.service.MemberService;
import com.dongsoop.dongsoop.restaurant.dto.RestaurantRegisterRequest;
import com.dongsoop.dongsoop.restaurant.entity.Restaurant;
import com.dongsoop.dongsoop.restaurant.entity.RestaurantCategory;
import com.dongsoop.dongsoop.restaurant.entity.RestaurantLike;
import com.dongsoop.dongsoop.restaurant.entity.RestaurantReport;
import com.dongsoop.dongsoop.restaurant.entity.RestaurantReportReason;
import com.dongsoop.dongsoop.restaurant.exception.RestaurantAlreadyExistsException;
import com.dongsoop.dongsoop.restaurant.exception.RestaurantNotFoundException;
import com.dongsoop.dongsoop.restaurant.repository.RestaurantLikeRepository;
import com.dongsoop.dongsoop.restaurant.repository.RestaurantReportRepository;
import com.dongsoop.dongsoop.restaurant.repository.RestaurantRepository;
import com.dongsoop.dongsoop.restaurant.service.RestaurantMapper;
import com.dongsoop.dongsoop.restaurant.service.RestaurantServiceImpl;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class RestaurantServiceImplTest {
    @Mock private RestaurantRepository restaurants;
    @Mock private RestaurantLikeRepository likes;
    @Mock private MemberRepository members;
    @Mock private RestaurantReportRepository reports;
    @Mock private MemberService memberService;
    private RestaurantServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new RestaurantServiceImpl(restaurants, likes, members,
                new RestaurantMapper(), reports, memberService);
    }

    @Test
    @DisplayName("신규 식당은 지도 ID와 입력 정보를 보존해 저장한다")
    void registersNewRestaurant() {
        RestaurantRegisterRequest request = request();
        when(restaurants.save(any(Restaurant.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Restaurant result = service.registerRestaurant(request);

        assertEquals(request.externalMapId(), result.getExternalMapId());
        assertEquals(request.name(), result.getName());
        assertEquals(request.placeUrl(), result.getPlaceUrl());
        assertEquals(request.distance(), result.getDistance());
        assertEquals(request.category(), result.getCategory());
        assertEquals(request.tags(), result.getTags());
    }

    @Test
    @DisplayName("활성 식당과 지도 ID가 같으면 저장하지 않는다")
    void rejectsDuplicateRestaurant() {
        when(restaurants.existsActiveByExternalMapId("map-1")).thenReturn(true);

        assertThrows(RestaurantAlreadyExistsException.class, () -> service.registerRestaurant(request()));

        verify(restaurants, never()).save(any());
    }

    @Test
    @DisplayName("회원 조회에는 현재 회원 ID와 카테고리 및 페이지를 전달한다")
    void authenticatedListing() {
        PageRequest page = PageRequest.of(1, 10);
        when(memberService.getMemberIdByAuthentication()).thenReturn(7L);
        when(restaurants.findNearbyRestaurants(7L, RestaurantCategory.KOREAN, page)).thenReturn(List.of());

        assertEquals(List.of(), service.getNearbyRestaurants(RestaurantCategory.KOREAN, page));

        verify(restaurants).findNearbyRestaurants(7L, RestaurantCategory.KOREAN, page);
    }

    @Test
    @DisplayName("인증 정보가 없으면 회원 ID 없이 조회한다")
    void guestListing() {
        PageRequest page = PageRequest.of(0, 10);
        when(memberService.getMemberIdByAuthentication()).thenThrow(new NotAuthenticationException());
        when(restaurants.findNearbyRestaurants(null, null, page)).thenReturn(List.of());

        assertEquals(List.of(), service.getNearbyRestaurants(null, page));

        verify(restaurants).findNearbyRestaurants(null, null, page);
    }

    @ParameterizedTest(name = "추가 요청={0}, 현재 좋아요={1}")
    @CsvSource({"true,false,1,0", "true,true,0,0", "false,true,0,1", "false,false,0,0"})
    @DisplayName("좋아요 추가·삭제는 현재 상태에 따라 필요한 저장만 수행한다")
    void likeRequestsAreIdempotent(boolean adding, boolean currentlyLiked, int expectedSaves, int expectedDeletes) {
        Restaurant restaurant = Restaurant.builder().id(3L).build();
        Member member = Member.builder().id(7L).build();
        RestaurantLike.RestaurantLikeKey key = new RestaurantLike.RestaurantLikeKey(3L, 7L);
        when(restaurants.findById(3L)).thenReturn(Optional.of(restaurant));
        when(members.getReferenceById(7L)).thenReturn(member);
        when(likes.existsById(key)).thenReturn(currentlyLiked);

        service.toggleLike(3L, 7L, adding);

        ArgumentCaptor<RestaurantLike> captor = ArgumentCaptor.forClass(RestaurantLike.class);
        verify(likes, times(expectedSaves)).save(captor.capture());
        verify(likes, times(expectedDeletes)).deleteById(key);
        if (expectedSaves == 1) {
            assertSame(restaurant, org.springframework.test.util.ReflectionTestUtils.getField(captor.getValue(), "restaurant"));
            assertSame(member, org.springframework.test.util.ReflectionTestUtils.getField(captor.getValue(), "member"));
        }
    }

    @Test
    @DisplayName("없는 식당의 좋아요는 회원 조회와 저장 전에 거절한다")
    void missingRestaurantCannotBeLiked() {
        assertThrows(RestaurantNotFoundException.class, () -> service.toggleLike(3L, 7L, true));
        verifyNoInteractions(likes, members);
    }

    @Test
    @DisplayName("신고에는 식당·신고자·사유·설명이 저장된다")
    void savesRestaurantReport() {
        Restaurant restaurant = Restaurant.builder().id(3L).build();
        Member reporter = Member.builder().id(7L).build();
        when(restaurants.findById(3L)).thenReturn(Optional.of(restaurant));
        when(members.getReferenceById(7L)).thenReturn(reporter);

        service.createRestaurantReport(3L, 7L, RestaurantReportReason.STORE_CLOSED, "영업 종료");

        ArgumentCaptor<RestaurantReport> captor = ArgumentCaptor.forClass(RestaurantReport.class);
        verify(reports).save(captor.capture());
        assertSame(restaurant, captor.getValue().getRestaurant());
        assertSame(reporter, captor.getValue().getReporter());
        assertEquals(RestaurantReportReason.STORE_CLOSED, captor.getValue().getReason());
        assertEquals("영업 종료", captor.getValue().getDescription());
    }

    @Test
    @DisplayName("없는 식당은 신고할 수 없다")
    void missingRestaurantCannotBeReported() {
        assertThrows(RestaurantNotFoundException.class,
                () -> service.createRestaurantReport(3L, 7L, RestaurantReportReason.OTHER, "설명"));
        verifyNoInteractions(reports, members);
    }

    @ParameterizedTest
    @CsvSource({"true", "false"})
    @DisplayName("지도 ID 중복 여부는 활성 식당 조회 결과를 따른다")
    void checksDuplicateId(boolean exists) {
        when(restaurants.existsActiveByExternalMapId("map-1")).thenReturn(exists);
        assertEquals(exists, service.checkDuplicateByExternalId("map-1"));
    }

    private RestaurantRegisterRequest request() {
        return new RestaurantRegisterRequest("map-1", "식당", "https://example.com/place", 120.0,
                RestaurantCategory.KOREAN, List.of());
    }
}
