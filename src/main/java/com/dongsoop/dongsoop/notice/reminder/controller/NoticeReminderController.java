package com.dongsoop.dongsoop.notice.reminder.controller;

import com.dongsoop.dongsoop.notice.reminder.dto.NoticeReminderRequest;
import com.dongsoop.dongsoop.notice.reminder.dto.NoticeReminderResponse;
import com.dongsoop.dongsoop.notice.reminder.service.NoticeReminderService;
import com.dongsoop.dongsoop.role.entity.RoleType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/notice/{noticeId}/reminder")
@RequiredArgsConstructor
@Secured(RoleType.USER_ROLE)
public class NoticeReminderController {

    private final NoticeReminderService noticeReminderService;

    @PutMapping
    public ResponseEntity<NoticeReminderResponse> upsert(
            @PathVariable Long noticeId,
            @RequestBody @Valid NoticeReminderRequest request
    ) {
        return ResponseEntity.ok(noticeReminderService.upsert(noticeId, request.remindAt()));
    }

    @GetMapping
    public ResponseEntity<NoticeReminderResponse> get(@PathVariable Long noticeId) {
        return ResponseEntity.ok(noticeReminderService.get(noticeId));
    }

    @DeleteMapping
    public ResponseEntity<Void> delete(@PathVariable Long noticeId) {
        noticeReminderService.delete(noticeId);
        return ResponseEntity.noContent().build();
    }
}
