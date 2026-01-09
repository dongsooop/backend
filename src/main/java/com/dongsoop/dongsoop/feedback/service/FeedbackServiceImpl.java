package com.dongsoop.dongsoop.feedback.service;

import com.dongsoop.dongsoop.feedback.dto.FeedbackCreate;
import com.dongsoop.dongsoop.feedback.dto.FeedbackDetail;
import com.dongsoop.dongsoop.feedback.dto.FeedbackOverview;
import com.dongsoop.dongsoop.feedback.entity.Feedback;
import com.dongsoop.dongsoop.feedback.entity.Feedback.FeedbackBuilder;
import com.dongsoop.dongsoop.feedback.entity.FeedbackServiceFeature;
import com.dongsoop.dongsoop.feedback.exception.FeedbackNotFoundException;
import com.dongsoop.dongsoop.feedback.notification.FeedbackNotification;
import com.dongsoop.dongsoop.feedback.repository.FeedbackRepository;
import com.dongsoop.dongsoop.feedback.repository.FeedbackServiceFeatureRepository;
import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.member.exception.MemberNotFoundException;
import com.dongsoop.dongsoop.member.service.MemberService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class FeedbackServiceImpl implements FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final FeedbackServiceFeatureRepository feedbackServiceFeatureRepository;
    private final FeedbackNotification feedbackNotification;
    private final MemberService memberService;

    @Override
    @Transactional
    public Long submitFeedback(FeedbackCreate request) {
        FeedbackBuilder feedbackBuilder = Feedback.builder()
                .improvementSuggestions(request.improvementSuggestions())
                .featureRequests(request.featureRequests());
        try {
            Member member = memberService.getMemberReferenceByContext();

            feedbackBuilder.member(member);
        } catch (MemberNotFoundException e) {
            // 회원이 존재하지 않으면 익명 피드백으로 처리
            log.debug("Anonymous feedback submitted - member not found in context");
        }

        Feedback feedback = feedbackRepository.save(feedbackBuilder.build());
        List<FeedbackServiceFeature> feedbackServiceFeature = request.feature()
                .stream()
                .map((feature) -> new FeedbackServiceFeature(feedback, feature))
                .toList();

        feedbackServiceFeatureRepository.saveAll(feedbackServiceFeature);

        feedbackNotification.send(request.improvementSuggestions());

        return feedback.getId();
    }

    @Override
    @Transactional(readOnly = true)
    public FeedbackDetail getFeedbackDetail(Long id) {
        return feedbackRepository.searchFeedbackById(id)
                .orElseThrow(FeedbackNotFoundException::new);
    }

    @Override
    @Transactional(readOnly = true)
    public FeedbackOverview getFeedbackOverview() {
        return feedbackRepository.searchFeedbackOverview();
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getImprovementSuggestions(Pageable pageable) {
        return feedbackRepository.searchAllImprovementSuggestions(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getFeatureRequests(Pageable pageable) {
        return feedbackRepository.searchAllFeatureRequests(pageable);
    }
}
