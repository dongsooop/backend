package com.dongsoop.dongsoop.notice.keyword.controller;

import com.dongsoop.dongsoop.notice.keyword.dto.NoticeKeywordRequest;
import com.dongsoop.dongsoop.notice.keyword.dto.NoticeKeywordResponse;
import com.dongsoop.dongsoop.notice.keyword.service.NoticeKeywordService;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/notice/keywords")
public class NoticeKeywordController {

    private final NoticeKeywordService noticeKeywordService;

    @GetMapping
    public ResponseEntity<List<NoticeKeywordResponse>> getKeywords() {
        List<NoticeKeywordResponse> keywords = noticeKeywordService.getKeywords();
        return ResponseEntity.ok(keywords);
    }

    @PostMapping
    public ResponseEntity<NoticeKeywordResponse> addKeyword(@Valid @RequestBody NoticeKeywordRequest request) {
        NoticeKeywordResponse response = noticeKeywordService.addKeyword(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{keywordId}")
    public ResponseEntity<Void> deleteKeyword(@PathVariable Long keywordId) {
        noticeKeywordService.deleteKeyword(keywordId);
        return ResponseEntity.noContent().build();
    }
}
