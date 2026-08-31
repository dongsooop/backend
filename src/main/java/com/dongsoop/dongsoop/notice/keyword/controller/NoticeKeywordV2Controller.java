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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 기기 단위 키워드 설정 엔드포인트.
 *
 * <p>회원 여부와 무관하게 기기 헤더로 대상을 정하므로 비회원도 사용할 수 있다.
 * 헤더는 {@code /subscribe-department} 와 같은 규칙을 쓴다 — fid 를 우선 보고, 없으면 deviceToken 으로 찾는다.
 *
 * <p>인증 기반으로 동작하던 {@link NoticeKeywordController} 는 구버전 앱을 위해 남겨둔다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/v2/notice/keywords")
public class NoticeKeywordV2Controller {

    private final NoticeKeywordService noticeKeywordService;

    @GetMapping
    public ResponseEntity<List<NoticeKeywordResponse>> getKeywords(
            @RequestHeader(value = "X-Device-Fid", required = false) String fid,
            @RequestHeader(value = "X-Device-Token", required = false) String deviceToken) {
        List<NoticeKeywordResponse> keywords = noticeKeywordService.getKeywords(fid, deviceToken);
        return ResponseEntity.ok(keywords);
    }

    @PostMapping
    public ResponseEntity<NoticeKeywordResponse> addKeyword(
            @RequestHeader(value = "X-Device-Fid", required = false) String fid,
            @RequestHeader(value = "X-Device-Token", required = false) String deviceToken,
            @Valid @RequestBody NoticeKeywordRequest request) {
        NoticeKeywordResponse response = noticeKeywordService.addKeyword(fid, deviceToken, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{keywordId}")
    public ResponseEntity<Void> deleteKeyword(
            @RequestHeader(value = "X-Device-Fid", required = false) String fid,
            @RequestHeader(value = "X-Device-Token", required = false) String deviceToken,
            @PathVariable Long keywordId) {
        noticeKeywordService.deleteKeyword(fid, deviceToken, keywordId);
        return ResponseEntity.noContent().build();
    }
}
