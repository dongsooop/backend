package com.dongsoop.dongsoop.notice.keyword.service;

import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.member.repository.MemberRepository;
import com.dongsoop.dongsoop.member.service.MemberService;
import com.dongsoop.dongsoop.notice.keyword.dto.NoticeKeywordRequest;
import com.dongsoop.dongsoop.notice.keyword.dto.NoticeKeywordResponse;
import com.dongsoop.dongsoop.notice.keyword.entity.NoticeKeyword;
import com.dongsoop.dongsoop.notice.keyword.entity.NoticeKeywordType;
import com.dongsoop.dongsoop.notice.keyword.exception.DuplicateNoticeKeywordException;
import com.dongsoop.dongsoop.notice.keyword.exception.NoticeKeywordNotFoundException;
import com.dongsoop.dongsoop.notice.keyword.repository.NoticeKeywordRepository;
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
    private final MemberRepository memberRepository;
    private final MemberService memberService;

    @Override
    @Transactional
    public NoticeKeywordResponse addKeyword(NoticeKeywordRequest request) {
        Long memberId = memberService.getMemberIdByAuthentication();

        if (noticeKeywordRepository.existsByMemberIdAndKeywordAndType(memberId, request.keyword(), request.type())) {
            throw new DuplicateNoticeKeywordException(request.keyword());
        }

        Member member = memberRepository.getReferenceById(memberId);
        NoticeKeyword keyword = new NoticeKeyword(member, request.keyword(), request.type());
        noticeKeywordRepository.save(keyword);

        return NoticeKeywordResponse.from(keyword);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NoticeKeywordResponse> getKeywords() {
        Long memberId = memberService.getMemberIdByAuthentication();

        return noticeKeywordRepository.findAllByMemberId(memberId).stream()
                .map(NoticeKeywordResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public void deleteKeyword(Long keywordId) {
        Long memberId = memberService.getMemberIdByAuthentication();

        NoticeKeyword keyword = noticeKeywordRepository.findByIdAndMemberId(keywordId, memberId)
                .orElseThrow(() -> new NoticeKeywordNotFoundException(keywordId));

        noticeKeywordRepository.delete(keyword);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Member> filterMembersByKeyword(List<Member> members, String noticeTitle) {
        if (members.isEmpty()) {
            return members;
        }

        List<Long> memberIds = members.stream()
                .map(Member::getId)
                .toList();

        List<NoticeKeyword> allKeywords = noticeKeywordRepository.findAllByMemberIdIn(memberIds);

        Map<Long, List<NoticeKeyword>> keywordsByMemberId = allKeywords.stream()
                .collect(Collectors.groupingBy(kw -> kw.getMember().getId()));

        return members.stream()
                .filter(member -> shouldReceiveNotification(keywordsByMemberId.get(member.getId()), noticeTitle))
                .toList();
    }

    private boolean shouldReceiveNotification(List<NoticeKeyword> keywords, String noticeTitle) {
        if (keywords == null || keywords.isEmpty()) {
            return true;
        }

        String titleLower = noticeTitle.toLowerCase();

        List<String> includeKeywords = keywords.stream()
                .filter(kw -> kw.getType() == NoticeKeywordType.INCLUDE)
                .map(kw -> kw.getKeyword().toLowerCase())
                .toList();

        List<String> excludeKeywords = keywords.stream()
                .filter(kw -> kw.getType() == NoticeKeywordType.EXCLUDE)
                .map(kw -> kw.getKeyword().toLowerCase())
                .toList();

        boolean hasExcludeMatch = excludeKeywords.stream().anyMatch(titleLower::contains);
        if (hasExcludeMatch) {
            return false;
        }

        if (!includeKeywords.isEmpty()) {
            return includeKeywords.stream().anyMatch(titleLower::contains);
        }

        return true;
    }
}
