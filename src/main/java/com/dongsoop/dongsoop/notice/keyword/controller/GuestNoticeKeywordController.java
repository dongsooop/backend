package com.dongsoop.dongsoop.notice.keyword.controller;

import com.dongsoop.dongsoop.notice.keyword.dto.NoticeKeywordRequest;
import com.dongsoop.dongsoop.notice.keyword.dto.NoticeKeywordResponse;
import com.dongsoop.dongsoop.notice.keyword.service.GuestNoticeKeywordService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/guest/notice/keywords")
@RequiredArgsConstructor
public class GuestNoticeKeywordController {

    private final GuestNoticeKeywordService guestNoticeKeywordService;

    @GetMapping
    public ResponseEntity<List<NoticeKeywordResponse>> getKeywords(
            @RequestHeader(value = "X-Anonymous-Key", required = false) String anonymousKey) {
        return ResponseEntity.ok(guestNoticeKeywordService.getKeywords(anonymousKey));
    }

    @PostMapping
    public ResponseEntity<NoticeKeywordResponse> addKeyword(
            @RequestHeader(value = "X-Anonymous-Key", required = false) String anonymousKey,
            @Valid @RequestBody NoticeKeywordRequest request) {
        NoticeKeywordResponse response = guestNoticeKeywordService.addKeyword(anonymousKey, request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(response);
    }

    @DeleteMapping("/{keywordId}")
    public ResponseEntity<Void> deleteKeyword(
            @RequestHeader(value = "X-Anonymous-Key", required = false) String anonymousKey,
            @PathVariable Long keywordId) {
        guestNoticeKeywordService.deleteKeyword(anonymousKey, keywordId);

        return ResponseEntity.noContent()
                .build();
    }
}
