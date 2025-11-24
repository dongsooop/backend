package com.dongsoop.dongsoop.restaurant.controller;

import com.dongsoop.dongsoop.member.service.MemberService;
import com.dongsoop.dongsoop.restaurant.dto.ReportWrongInfoRequest;
import com.dongsoop.dongsoop.restaurant.dto.RestaurantOverview;
import com.dongsoop.dongsoop.restaurant.dto.RestaurantRegisterRequest;
import com.dongsoop.dongsoop.restaurant.entity.RestaurantReportReason;
import com.dongsoop.dongsoop.restaurant.entity.RestaurantStatus;
import com.dongsoop.dongsoop.restaurant.service.RestaurantService;
import com.dongsoop.dongsoop.role.entity.RoleType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/restaurants")
@RequiredArgsConstructor
public class RestaurantController {

    private final RestaurantService restaurantService;
    private final MemberService memberService;

    @PostMapping("/register")
    @Secured(RoleType.USER_ROLE)
    public ResponseEntity<Void> registerRestaurant(@RequestBody @Valid RestaurantRegisterRequest request) {
        restaurantService.registerRestaurant(request);
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/nearby")
    public ResponseEntity<List<RestaurantOverview>> getNearbyRestaurants(Pageable pageable) {
        List<RestaurantOverview> restaurants = restaurantService.getNearbyRestaurants(pageable);
        return ResponseEntity.ok(restaurants);
    }

    @PostMapping("/{restaurantId}/like/toggle")
    @Secured(RoleType.USER_ROLE)
    public ResponseEntity<Void> toggleLike(@PathVariable Long restaurantId, @RequestParam boolean isAdding) {
        Long memberId = memberService.getMemberIdByAuthentication();
        restaurantService.toggleLike(restaurantId, memberId, isAdding);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/admin/pending")
    @Secured(RoleType.ADMIN_ROLE)
    public ResponseEntity<List<RestaurantOverview>> getPendingRestaurants(Pageable pageable) {
        Long adminId = memberService.getMemberIdByAuthentication();
        List<RestaurantOverview> restaurants = restaurantService.getRestaurantsByStatus(RestaurantStatus.PENDING, adminId, pageable);
        return ResponseEntity.ok(restaurants);
    }

    @PostMapping("/admin/approve/{restaurantId}")
    @Secured(RoleType.ADMIN_ROLE)
    public ResponseEntity<Void> approveRestaurant(@PathVariable Long restaurantId) {
        restaurantService.approveRestaurant(restaurantId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/admin/reject/{restaurantId}")
    @Secured(RoleType.ADMIN_ROLE)
    public ResponseEntity<Void> rejectRestaurant(@PathVariable Long restaurantId) {
        restaurantService.rejectRestaurant(restaurantId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{restaurantId}/report-closed")
    @Secured(RoleType.USER_ROLE)
    public ResponseEntity<Void> reportClosed(@PathVariable Long restaurantId) {
        Long reporterId = memberService.getMemberIdByAuthentication();

        restaurantService.createRestaurantReport(
                restaurantId,
                reporterId,
                RestaurantReportReason.STORE_CLOSED,
                "가게 폐업 신고"
        );

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/{restaurantId}/report-wrong-info")
    @Secured(RoleType.USER_ROLE)
    public ResponseEntity<Void> reportWrongInfo(
            @PathVariable Long restaurantId,
            @RequestBody @Valid ReportWrongInfoRequest request
    ) {
        Long reporterId = memberService.getMemberIdByAuthentication();
        String description = "잘못된 정보 신고";

        if (request.description() != null && !request.description().isBlank()) {
            description = request.description();
        }

        restaurantService.createRestaurantReport(
                restaurantId,
                reporterId,
                RestaurantReportReason.WRONG_INFORMATION,
                description
        );

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/validate/external-id")
    public ResponseEntity<Map<String, Boolean>> checkDuplicateExternalId(@RequestParam String externalMapId) {
        Map<String, Boolean> response = Map.of("isDuplicate", restaurantService.checkDuplicateByExternalId(externalMapId));
        return ResponseEntity.ok(response);
    }
}