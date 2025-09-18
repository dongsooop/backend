package com.dongsoop.dongsoop.search.dto;

import com.dongsoop.dongsoop.search.entity.BoardDocument;
import com.dongsoop.dongsoop.search.entity.BoardType;
import org.springframework.data.domain.Page;

import java.util.List;

public class SearchDtoMapper {

    public static BoardSearchResult toBoardSearchResult(BoardDocument document) {
        return BoardSearchResult.builder()
                .id(document.getId())
                .title(document.getTitle())
                .content(getContentByBoardType(document))
                .boardType(document.getBoardType())
                .boardId(document.getBoardId())
                .authorName(document.getAuthorName())
                .createdAt(document.getCreatedAt())
                .recruitmentStartAt(document.getRecruitmentStartAt())
                .recruitmentEndAt(document.getRecruitmentEndAt())
                .marketplaceStatus(document.getMarketplaceStatus())
                .marketplaceType(document.getMarketplaceType())
                .noticeUrl(getNoticeUrlByBoardType(document))
                .tags(document.getTags())
                .price(document.getPrice())
                .departmentName(document.getDepartmentName())
                .contactCount(document.getContactCount())
                .build();
    }

    public static SearchResponse toSearchResponse(Page<BoardDocument> page) {
        List<BoardSearchResult> results = page.getContent().stream()
                .map(SearchDtoMapper::toBoardSearchResult)
                .toList();

        return new SearchResponse(
                results,
                (int) page.getTotalElements(),
                page.getTotalPages(),
                page.getNumber(),
                page.getSize()
        );
    }

    private static String getContentByBoardType(BoardDocument document) {
        if (BoardType.NOTICE.getCode().equals(document.getBoardType())) {
            return null;
        }
        return document.getContent();
    }

    private static String getNoticeUrlByBoardType(BoardDocument document) {
        if (BoardType.NOTICE.getCode().equals(document.getBoardType())) {
            return document.getNoticeUrl();
        }
        return null;
    }
}
