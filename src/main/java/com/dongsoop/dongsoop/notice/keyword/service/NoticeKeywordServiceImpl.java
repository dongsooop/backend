package com.dongsoop.dongsoop.notice.keyword.service;

import com.dongsoop.dongsoop.member.service.MemberService;
import com.dongsoop.dongsoop.memberdevice.entity.MemberDevice;
import com.dongsoop.dongsoop.memberdevice.exception.UnregisteredDeviceException;
import com.dongsoop.dongsoop.memberdevice.repository.MemberDeviceRepository;
import com.dongsoop.dongsoop.memberdevice.service.NoticePreferenceDeviceResolver;
import com.dongsoop.dongsoop.notice.keyword.dto.NoticeKeywordRequest;
import com.dongsoop.dongsoop.notice.keyword.dto.NoticeKeywordResponse;
import com.dongsoop.dongsoop.notice.keyword.entity.NoticeKeyword;
import com.dongsoop.dongsoop.notice.keyword.exception.DuplicateNoticeKeywordException;
import com.dongsoop.dongsoop.notice.keyword.exception.NoticeKeywordNotFoundException;
import com.dongsoop.dongsoop.notice.keyword.repository.NoticeKeywordRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NoticeKeywordServiceImpl implements NoticeKeywordService {

    private final NoticeKeywordRepository noticeKeywordRepository;
    private final NoticePreferenceDeviceResolver deviceResolver;
    private final MemberDeviceRepository memberDeviceRepository;
    private final MemberService memberService;

    // ===== 구버전 API (/notice/keywords) — 인증된 회원의 기기 전체 =====

    @Override
    @Transactional(readOnly = true)
    public List<NoticeKeywordResponse> getKeywordsByMember() {
        return getKeywordsOf(devicesOfAuthenticatedMember());
    }

    @Override
    @Transactional
    public NoticeKeywordResponse addKeywordByMember(NoticeKeywordRequest request) {
        return addKeywordTo(devicesOfAuthenticatedMember(), request);
    }

    @Override
    @Transactional
    public void deleteKeywordByMember(Long keywordId) {
        deleteKeywordFrom(devicesOfAuthenticatedMember(), keywordId);
    }

    // ===== 신버전 API (/v2/notice/keywords) — 기기 하나 =====

    @Override
    @Transactional(readOnly = true)
    public List<NoticeKeywordResponse> getKeywords(String fid, String deviceToken) {
        return getKeywordsOf(List.of(deviceResolver.resolve(fid, deviceToken)));
    }

    @Override
    @Transactional
    public NoticeKeywordResponse addKeyword(String fid, String deviceToken, NoticeKeywordRequest request) {
        return addKeywordTo(List.of(deviceResolver.resolve(fid, deviceToken)), request);
    }

    @Override
    @Transactional
    public void deleteKeyword(String fid, String deviceToken, Long keywordId) {
        deleteKeywordFrom(List.of(deviceResolver.resolve(fid, deviceToken)), keywordId);
    }

    // ===== 대상 기기가 정해진 뒤의 공통 처리 =====

    /**
     * 구버전 API 의 대상 기기.
     *
     * <p>기기 헤더를 보내지 않던 시절의 앱은 인증 토큰만 보낸다. 키워드가 기기 단위로 바뀌기 전에는
     * 회원 하나에 키워드 묶음 하나였으므로, 그 회원의 기기 전체를 대상으로 삼아야 예전처럼 동작한다.
     * 구버전 앱이 줄어들면 이 메서드와 이를 쓰는 세 메서드, 그리고 구버전 컨트롤러를 지우면 된다.
     */
    private List<MemberDevice> devicesOfAuthenticatedMember() {
        Long memberId = memberService.getMemberIdByAuthentication();
        List<MemberDevice> devices = memberDeviceRepository.findByMemberId(memberId);

        if (devices.isEmpty()) {
            throw new UnregisteredDeviceException();
        }

        return devices;
    }

    private List<NoticeKeywordResponse> getKeywordsOf(List<MemberDevice> devices) {
        List<Long> deviceIds = toDeviceIds(devices);

        // 기기마다 설정이 다를 수 있으므로 (keyword, type) 기준으로 합쳐서 돌려준다.
        // 대상이 기기 하나면 중복이 없어 그대로 나간다
        return noticeKeywordRepository.findAllByDeviceIdIn(deviceIds).stream()
                .collect(Collectors.toMap(
                        keyword -> Map.entry(keyword.getKeyword(), keyword.getType()),
                        keyword -> keyword,
                        (existing, duplicate) -> existing,
                        LinkedHashMap::new))
                .values().stream()
                .map(NoticeKeywordResponse::from)
                .toList();
    }

    private NoticeKeywordResponse addKeywordTo(List<MemberDevice> devices, NoticeKeywordRequest request) {
        boolean duplicated = devices.stream()
                .anyMatch(device -> noticeKeywordRepository.existsByDeviceIdAndKeywordAndType(
                        device.getId(), request.keyword(), request.type()));
        if (duplicated) {
            throw new DuplicateNoticeKeywordException(request.keyword());
        }

        List<NoticeKeyword> keywords = devices.stream()
                .map(device -> new NoticeKeyword(device, request.keyword(), request.type()))
                .toList();
        noticeKeywordRepository.saveAll(keywords);

        // 대상이 여러 기기여도 응답은 한 건이다. 기기마다 같은 (keyword, type) 이라 어느 것을 돌려줘도 같다
        return NoticeKeywordResponse.from(keywords.get(0));
    }

    private void deleteKeywordFrom(List<MemberDevice> devices, Long keywordId) {
        List<Long> deviceIds = toDeviceIds(devices);

        NoticeKeyword keyword = noticeKeywordRepository.findByIdAndDeviceIdIn(keywordId, deviceIds)
                .orElseThrow(() -> new NoticeKeywordNotFoundException(keywordId));

        // 대상 기기 전체에서 같은 (keyword, type) 을 지운다.
        // 구버전 앱은 목록에서 받은 id 하나로 삭제를 요청하는데 그 id 는 특정 기기의 행이라,
        // 그것만 지우면 나머지 기기에 남아 알림이 계속 온다
        noticeKeywordRepository.deleteByDeviceIdInAndKeywordAndType(deviceIds, keyword.getKeyword(),
                keyword.getType());
    }

    private List<Long> toDeviceIds(List<MemberDevice> devices) {
        return devices.stream()
                .map(MemberDevice::getId)
                .distinct()
                .toList();
    }

    /**
     * 대상 기기들의 키워드 설정을 한 번에 읽어 필터를 만든다.
     * 반환된 필터는 같은 크롤링에서 나온 공지 여러 건에 재사용한다.
     */
    @Override
    @Transactional(readOnly = true)
    public NoticeKeywordFilter loadFilter(List<MemberDevice> devices) {
        if (devices.isEmpty()) {
            return new NoticeKeywordFilter(Map.of());
        }

        // 같은 기기가 여러 학과를 구독했으면 목록에 중복으로 들어오므로 id 를 추린다
        List<Long> deviceIds = toDeviceIds(devices);

        Map<Long, List<NoticeKeyword>> keywordsByDeviceId = noticeKeywordRepository.findAllByDeviceIdIn(deviceIds)
                .stream()
                .collect(Collectors.groupingBy(keyword -> keyword.getDevice().getId()));

        return new NoticeKeywordFilter(keywordsByDeviceId);
    }
}
