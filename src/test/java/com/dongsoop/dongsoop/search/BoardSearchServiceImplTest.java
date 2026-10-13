package com.dongsoop.dongsoop.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.department.entity.Department;
import com.dongsoop.dongsoop.marketplace.entity.MarketplaceType;
import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.member.service.MemberService;
import com.dongsoop.dongsoop.search.dto.BoardSearchResult;
import com.dongsoop.dongsoop.search.dto.RestaurantSearchResult;
import com.dongsoop.dongsoop.search.dto.SearchResponse;
import com.dongsoop.dongsoop.search.entity.BoardDocument;
import com.dongsoop.dongsoop.search.entity.BoardType;
import com.dongsoop.dongsoop.search.entity.RestaurantDocument;
import com.dongsoop.dongsoop.search.repository.BoardSearchRepository;
import com.dongsoop.dongsoop.search.repository.RestaurantSearchRepository;
import com.dongsoop.dongsoop.search.service.BoardSearchServiceImpl;
import com.dongsoop.dongsoop.search.service.PopularKeywordService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class BoardSearchServiceImplTest {
    @Mock private BoardSearchRepository boards;
    @Mock private RestaurantSearchRepository restaurants;
    @Mock private PopularKeywordService popular;
    @Mock private MemberService members;
    private BoardSearchServiceImpl service;
    private static final PageRequest PAGE = PageRequest.of(1, 10);

    @BeforeEach
    void setUp() {
        service = new BoardSearchServiceImpl(boards, restaurants, popular, members);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t\n"})
    @DisplayName("빈 검색어는 저장소와 인기 검색어를 호출하지 않고 요청 페이지의 빈 결과를 반환한다")
    void emptyKeywordsAvoidDependencies(String keyword) {
        assertTrue(service.searchByBoardType(keyword, null, null, null, PAGE).isEmpty());
        SearchResponse<BoardSearchResult> noticeResult = service.searchNoticesByDepartment(keyword, "학과", PAGE);
        SearchResponse<RestaurantSearchResult> restaurantResult = service.searchRestaurants(keyword, PAGE);
        assertEquals(List.of(), noticeResult.getResults());
        assertEquals(PAGE.getPageNumber(), noticeResult.getCurrentPage());
        assertEquals(PAGE.getPageSize(), noticeResult.getPageSize());
        assertEquals(List.of(), restaurantResult.getResults());
        assertEquals(PAGE.getPageNumber(), restaurantResult.getCurrentPage());
        assertEquals(0, restaurantResult.getTotalElements());
        assertEquals(List.of(), service.getAutocompleteSuggestions(keyword, null));
        verifyNoInteractions(boards, restaurants, popular, members);
    }

    @Test
    @DisplayName("게시판 검색은 연속 공백을 정리하고 게시판·거래 유형·학과·페이지를 전달한다")
    void normalizesKeywordAndPassesFilters() {
        Page<BoardDocument> expected = new PageImpl<>(List.of(board("title")), PAGE, 11);
        when(boards.searchDynamic("Java Spring", List.of("PROJECT", "MARKETPLACE"), "SELL",
                "학과", null, PAGE)).thenReturn(expected);

        Page<BoardDocument> result = service.searchByBoardType("  Java \t Spring  ",
                List.of(BoardType.PROJECT, BoardType.MARKETPLACE), MarketplaceType.SELL, "학과", PAGE);

        assertSame(expected, result);
        verify(popular).updateKeywordScore("java spring");
    }

    @Test
    @DisplayName("필터가 없는 게시판 검색은 null 필터를 유지한다")
    void acceptsMissingFilters() {
        when(boards.searchDynamic("검색", null, null, null, null, PAGE)).thenReturn(Page.empty(PAGE));
        assertTrue(service.searchByBoardType("검색", null, null, null, PAGE).isEmpty());
        verify(boards).searchDynamic("검색", null, null, null, null, PAGE);
    }

    @Test
    @DisplayName("인기 검색어 Redis 갱신 실패가 검색 자체를 막지 않는다")
    void keywordTrackingFailureDoesNotBlockSearch() {
        doThrow(new IllegalStateException("Redis 실패")).when(popular).updateKeywordScore("java");
        when(boards.searchDynamic("Java", null, null, null, null, PAGE)).thenReturn(Page.empty(PAGE));
        assertTrue(service.searchByBoardType("Java", null, null, null, PAGE).isEmpty());
        verify(boards).searchDynamic("Java", null, null, null, null, PAGE);
    }

    @Test
    @DisplayName("일반 게시판 검색의 저장소 실패는 호출자에게 전달한다")
    void boardSearchPropagatesRepositoryFailure() {
        IllegalStateException cause = new IllegalStateException("검색 실패");
        when(boards.searchDynamic("Java", null, null, null, null, PAGE)).thenThrow(cause);
        assertSame(cause, assertThrows(IllegalStateException.class,
                () -> service.searchByBoardType("Java", null, null, null, PAGE)));
    }

    @Test
    @DisplayName("공지 검색은 NOTICE와 작성자 필터를 사용하고 링크 및 페이지 정보를 변환한다")
    void noticeSearchMapsResultAndPagination() {
        BoardDocument notice = new BoardDocument("notice-1", "공지", "비공개 본문", "NOTICE", 3L,
                "학과", "2026-10-13", null, null, null, null, "https://example.com/notice",
                null, null, null, null);
        when(boards.searchDynamic("공지", List.of("NOTICE"), null, null, "학과", PAGE))
                .thenReturn(new PageImpl<>(List.of(notice), PAGE, 11));

        SearchResponse<BoardSearchResult> result = service.searchNoticesByDepartment(" 공지 ", "학과", PAGE);

        assertEquals(11, result.getTotalElements());
        assertEquals(2, result.getTotalPages());
        assertEquals(1, result.getCurrentPage());
        assertEquals("https://example.com/notice", result.getResults().get(0).getNoticeUrl());
        assertNull(result.getResults().get(0).getContent());
    }

    @Test
    @DisplayName("공지 검색 실패는 요청 페이지의 빈 결과로 처리한다")
    void noticeSearchFailureReturnsEmptyPage() {
        when(boards.searchDynamic("공지", List.of("NOTICE"), null, null, "학과", PAGE))
                .thenThrow(new IllegalStateException("검색 실패"));
        SearchResponse<BoardSearchResult> result = service.searchNoticesByDepartment("공지", "학과", PAGE);
        assertEquals(List.of(), result.getResults());
        assertEquals(1, result.getCurrentPage());
        assertEquals(10, result.getPageSize());
    }

    @Test
    @DisplayName("식당 검색은 현재 회원의 좋아요와 태그·카테고리 및 페이지 정보를 반영한다")
    void restaurantSearchUsesAuthenticatedMember() {
        when(members.isAuthenticated()).thenReturn(true);
        when(members.getMemberIdByAuthentication()).thenReturn(7L);
        when(restaurants.searchByKeywordDynamic("식당", PAGE))
                .thenReturn(new PageImpl<>(List.of(restaurant("식당", "2,7")), PAGE, 11));

        SearchResponse<RestaurantSearchResult> result = service.searchRestaurants(" 식당 ", PAGE);

        RestaurantSearchResult item = result.getResults().get(0);
        assertTrue(item.getIsLikedByMe());
        assertEquals("한식", item.getCategory());
        assertEquals(List.of("혼밥", "가성비"), item.getTags());
        assertEquals(123L, item.getExternalMapId());
        assertEquals(11, result.getTotalElements());
        assertEquals(2, result.getTotalPages());
        verify(popular).updateKeywordScore("식당");
    }

    @Test
    @DisplayName("비회원 식당 검색은 개인 좋아요 없이 결과를 제공한다")
    void guestRestaurantSearch() {
        when(restaurants.searchByKeywordDynamic("식당", PAGE))
                .thenReturn(new PageImpl<>(List.of(restaurant("식당", "7")), PAGE, 11));
        assertFalse(service.searchRestaurants("식당", PAGE).getResults().get(0).getIsLikedByMe());
    }

    @Test
    @DisplayName("회원 정보 조회 실패가 식당 검색을 막지 않는다")
    void memberLookupFailureFallsBackToGuest() {
        when(members.isAuthenticated()).thenReturn(true);
        when(members.getMemberIdByAuthentication()).thenThrow(new IllegalStateException("인증 실패"));
        when(restaurants.searchByKeywordDynamic("식당", PAGE))
                .thenReturn(new PageImpl<>(List.of(restaurant("식당", "7")), PAGE, 11));
        assertFalse(service.searchRestaurants("식당", PAGE).getResults().get(0).getIsLikedByMe());
    }

    @Test
    @DisplayName("식당 검색 실패는 요청 페이지의 빈 결과로 처리한다")
    void restaurantSearchFailureReturnsEmptyPage() {
        when(restaurants.searchByKeywordDynamic("식당", PAGE)).thenThrow(new IllegalStateException("검색 실패"));
        SearchResponse<RestaurantSearchResult> result = service.searchRestaurants("식당", PAGE);
        assertEquals(List.of(), result.getResults());
        assertEquals(1, result.getCurrentPage());
        assertEquals(10, result.getPageSize());
    }

    @Test
    @DisplayName("식당 자동완성은 중복을 제거하고 접두어 일치를 우선 정렬한다")
    void restaurantAutocompleteSortsAndDeduplicates() {
        when(restaurants.findAutocompleteSuggestionsDynamic("cat", PageRequest.of(0, 10)))
                .thenReturn(List.of(restaurant("z cat", null), restaurant("cat beta", null),
                        restaurant("cat alpha", null), restaurant("A cat", null), restaurant("cat alpha", null)));
        assertEquals(List.of("cat alpha", "cat beta", "A cat", "z cat"),
                service.getAutocompleteSuggestions("cat", "restaurant"));
        verifyNoInteractions(boards);
    }

    @Test
    @DisplayName("게시판 자동완성은 지정된 게시판 유형으로 조회한다")
    void boardAutocompleteUsesType() {
        when(boards.findAutocompleteSuggestionsDynamic("cat", "PROJECT", PageRequest.of(0, 10)))
                .thenReturn(List.of(board("z cat"), board("cat first")));
        assertEquals(List.of("cat first", "z cat"), service.getAutocompleteSuggestions("cat", "PROJECT"));
        verifyNoInteractions(restaurants);
    }

    @Test
    @DisplayName("전체 자동완성은 게시판과 식당 후보를 합쳐 중복을 제거한다")
    void allAutocompleteMergesBothSources() {
        when(boards.findAutocompleteSuggestionsDynamic("cat", null, PageRequest.of(0, 5)))
                .thenReturn(List.of(board("cat one"), board("z cat")));
        when(restaurants.findAutocompleteSuggestionsDynamic("cat", PageRequest.of(0, 5)))
                .thenReturn(List.of(restaurant("cat one", null), restaurant("cat two", null)));
        assertEquals(List.of("cat one", "cat two", "z cat"), service.getAutocompleteSuggestions("cat", null));
    }

    @Test
    @DisplayName("공지 자동완성은 회원 소속 학과로 제한한다")
    void noticeAutocompleteUsesMemberDepartment() {
        when(members.isAuthenticated()).thenReturn(true);
        when(members.getMemberReferenceByContext())
                .thenReturn(Member.builder().department(new Department(DepartmentType.DEPT_2001,
                        DepartmentType.DEPT_2001.getName(), "https://example.com/notice")).build());
        when(boards.findNoticeAutocompleteSuggestionsDynamic("공지", DepartmentType.DEPT_2001.getName(),
                PageRequest.of(0, 10))).thenReturn(List.of(board("공지 제목")));
        assertEquals(List.of("공지 제목"), service.getAutocompleteSuggestions("공지", "NOTICE"));
    }

    @Test
    @DisplayName("비회원은 학과 공지 자동완성 저장소에 접근하지 않는다")
    void guestNoticeAutocompleteIsEmpty() {
        assertEquals(List.of(), service.getAutocompleteSuggestions("공지", "NOTICE"));
        verifyNoInteractions(boards, restaurants);
    }

    @Test
    @DisplayName("학과 조회에 실패하면 공지 자동완성은 빈 결과로 처리한다")
    void departmentLookupFailureReturnsEmptySuggestions() {
        when(members.isAuthenticated()).thenReturn(true);
        when(members.getMemberReferenceByContext()).thenThrow(new IllegalStateException("회원 실패"));
        assertEquals(List.of(), service.getAutocompleteSuggestions("공지", "NOTICE"));
        verifyNoInteractions(boards, restaurants);
    }

    private BoardDocument board(String title) {
        return new BoardDocument("board-1", title, "본문", "PROJECT", 3L, "작성자", "2026-10-13",
                null, null, null, null, null, null, null, null, null);
    }

    private RestaurantDocument restaurant(String name, String likedMembers) {
        return new RestaurantDocument("restaurant-1", 3L, name, "KOREAN", "혼밥, 가성비",
                "https://example.com/place", 2, 120.0, false, "123", likedMembers);
    }
}
