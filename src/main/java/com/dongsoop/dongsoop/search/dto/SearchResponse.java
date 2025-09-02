package com.dongsoop.dongsoop.search.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public class SearchResponse {
    private final List<BoardSearchResult> results;
    private final int totalElements;
    private final int totalPages;
    private final int currentPage;
    private final int pageSize;
}
