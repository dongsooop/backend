package com.dongsoop.dongsoop.search.dto;

import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class SearchResponse<T> {
  private final List<T> results;
  private final int totalElements;
  private final int totalPages;
  private final int currentPage;
  private final int pageSize;
}
