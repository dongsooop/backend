package com.dongsoop.dongsoop.notice.reminder.controller;

import com.dongsoop.dongsoop.notice.reminder.dto.NoticeReminderRequest;
import com.dongsoop.dongsoop.notice.reminder.dto.NoticeReminderResponse;
import com.dongsoop.dongsoop.notice.reminder.service.NoticeReminderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/notice/{noticeId}/reminder")
@RequiredArgsConstructor
public class NoticeReminderController {

    private final NoticeReminderService noticeReminderService;

    @PutMapping
    public ResponseEntity<NoticeReminderResponse> upsert(
            @PathVariable Long noticeId,
            @RequestHeader(value = "X-Device-Fid", required = false) String fid,
            @RequestHeader(value = "X-Device-Token", required = false) String deviceToken,
            @RequestBody @Valid NoticeReminderRequest request
    ) {
        return ResponseEntity.ok(noticeReminderService.upsert(noticeId, fid, deviceToken, request.remindAt()));
    }

    @GetMapping
    public ResponseEntity<NoticeReminderResponse> get(
            @PathVariable Long noticeId,
            @RequestHeader(value = "X-Device-Fid", required = false) String fid,
            @RequestHeader(value = "X-Device-Token", required = false) String deviceToken
    ) {
        return ResponseEntity.ok(noticeReminderService.get(noticeId, fid, deviceToken));
    }

    @DeleteMapping
    public ResponseEntity<Void> delete(
            @PathVariable Long noticeId,
            @RequestHeader(value = "X-Device-Fid", required = false) String fid,
            @RequestHeader(value = "X-Device-Token", required = false) String deviceToken
    ) {
        noticeReminderService.delete(noticeId, fid, deviceToken);
        return ResponseEntity.noContent().build();
    }
}
