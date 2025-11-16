package com.dongsoop.dongsoop.feedback.service;

import com.dongsoop.dongsoop.feedback.dto.FeedbackCreate;
import com.dongsoop.dongsoop.feedback.dto.FeedbackDetail;
import com.dongsoop.dongsoop.feedback.entity.Feedback;
import com.dongsoop.dongsoop.feedback.entity.Feedback.FeedbackBuilder;
import com.dongsoop.dongsoop.feedback.exception.FeedbackNotFoundException;
import com.dongsoop.dongsoop.feedback.repository.FeedbackRepository;
import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.member.exception.MemberNotFoundException;
import com.dongsoop.dongsoop.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FeedbackServiceImpl implements FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final MemberService memberService;

    @Override
    public Long submitFeedback(FeedbackCreate request) {
        FeedbackBuilder feedbackBuilder = Feedback.builder()
                .content(request.content());
        try {
            Member member = memberService.getMemberReferenceByContext();

            feedbackBuilder.member(member);
        } catch (MemberNotFoundException e) {
            // 회원이 존재하지 않으면 익명 피드백으로 처리
        }

        Feedback feedback = feedbackRepository.save(feedbackBuilder.build());

        return feedback.getId();
    }

    @Override
    public FeedbackDetail getFeedbackDetail(Long id) {
        return feedbackRepository.searchFeedbackById(id)
                .orElseThrow(FeedbackNotFoundException::new);
    }
}
