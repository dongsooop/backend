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

/**
 * 기기 헤더를 보내지 않는 구버전 앱용 엔드포인트.
 *
 * <p>키워드는 기기 단위로 저장되지만, 이 경로는 인증된 회원의 기기 전체를 한 묶음으로 다뤄
 * 회원 단위로 동작하던 예전 방식을 유지한다. 기기별로 다르게 두려면 {@link NoticeKeywordV2Controller} 를 쓴다.
 * 구버전 앱이 줄어들면 이 클래스를 통째로 지우면 된다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/notice/keywords")
public class NoticeKeywordController {

    private final NoticeKeywordService noticeKeywordService;

    @GetMapping
    public ResponseEntity<List<NoticeKeywordResponse>> getKeywords() {
        List<NoticeKeywordResponse> keywords = noticeKeywordService.getKeywordsByMember();
        return ResponseEntity.ok(keywords);
    }

    @PostMapping
    public ResponseEntity<NoticeKeywordResponse> addKeyword(@Valid @RequestBody NoticeKeywordRequest request) {
        NoticeKeywordResponse response = noticeKeywordService.addKeywordByMember(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{keywordId}")
    public ResponseEntity<Void> deleteKeyword(@PathVariable Long keywordId) {
        noticeKeywordService.deleteKeywordByMember(keywordId);
        return ResponseEntity.noContent().build();
    }
}
