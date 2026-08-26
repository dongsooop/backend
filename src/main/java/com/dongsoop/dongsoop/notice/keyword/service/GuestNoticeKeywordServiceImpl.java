package com.dongsoop.dongsoop.notice.keyword.service;

import com.dongsoop.dongsoop.memberdevice.entity.MemberDevice;
import com.dongsoop.dongsoop.memberdevice.service.GuestDeviceResolver;
import com.dongsoop.dongsoop.notice.keyword.dto.NoticeKeywordRequest;
import com.dongsoop.dongsoop.notice.keyword.dto.NoticeKeywordResponse;
import com.dongsoop.dongsoop.notice.keyword.entity.NoticeKeyword;
import com.dongsoop.dongsoop.notice.keyword.exception.DuplicateNoticeKeywordException;
import com.dongsoop.dongsoop.notice.keyword.exception.NoticeKeywordNotFoundException;
import com.dongsoop.dongsoop.notice.keyword.repository.NoticeKeywordRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GuestNoticeKeywordServiceImpl implements GuestNoticeKeywordService {

    private final GuestDeviceResolver guestDeviceResolver;
    private final NoticeKeywordRepository noticeKeywordRepository;

    @Override
    @Transactional(readOnly = true)
    public List<NoticeKeywordResponse> getKeywords(String anonymousKey) {
        MemberDevice device = guestDeviceResolver.resolve(anonymousKey);

        return noticeKeywordRepository.findAllByDeviceId(device.getId()).stream()
                .map(NoticeKeywordResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public NoticeKeywordResponse addKeyword(String anonymousKey, NoticeKeywordRequest request) {
        MemberDevice device = guestDeviceResolver.resolve(anonymousKey);

        if (noticeKeywordRepository.existsByDeviceIdAndKeywordAndType(device.getId(), request.keyword(),
                request.type())) {
            throw new DuplicateNoticeKeywordException(request.keyword());
        }

        NoticeKeyword keyword = new NoticeKeyword(device, request.keyword(), request.type());
        noticeKeywordRepository.save(keyword);

        return NoticeKeywordResponse.from(keyword);
    }

    @Override
    @Transactional
    public void deleteKeyword(String anonymousKey, Long keywordId) {
        MemberDevice device = guestDeviceResolver.resolve(anonymousKey);

        NoticeKeyword keyword = noticeKeywordRepository.findByIdAndDeviceId(keywordId, device.getId())
                .orElseThrow(() -> new NoticeKeywordNotFoundException(keywordId));

        noticeKeywordRepository.delete(keyword);
    }
}
