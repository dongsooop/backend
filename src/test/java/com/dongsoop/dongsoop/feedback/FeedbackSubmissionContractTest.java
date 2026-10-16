package com.dongsoop.dongsoop.feedback;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.dongsoop.dongsoop.feedback.dto.*;
import com.dongsoop.dongsoop.feedback.entity.*;
import com.dongsoop.dongsoop.feedback.exception.FeedbackNotFoundException;
import com.dongsoop.dongsoop.feedback.notification.FeedbackNotification;
import com.dongsoop.dongsoop.feedback.repository.*;
import com.dongsoop.dongsoop.feedback.service.FeedbackServiceImpl;
import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.member.exception.MemberNotFoundException;
import com.dongsoop.dongsoop.member.service.MemberService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class FeedbackSubmissionContractTest {
    @Mock FeedbackRepository feedbacks;
    @Mock FeedbackServiceFeatureRepository features;
    @Mock FeedbackNotification notification;
    @Mock MemberService members;
    @InjectMocks FeedbackServiceImpl service;

    @Test
    void memberSubmissionStoresPayloadAndSelectedFeaturesBeforeNotification() {
        var member = Member.builder().id(7L).build();
        var saved = Feedback.builder().id(19L).build();
        var request = new FeedbackCreate(List.of(ServiceFeature.CHATBOT_CAMPUS_INFO, ServiceFeature.NOTICE_ALERT), "Improve search", "New feature");
        when(members.getMemberReferenceByContext()).thenReturn(member);
        when(feedbacks.save(any(Feedback.class))).thenReturn(saved);

        assertThat(service.submitFeedback(request)).isEqualTo(19L);

        var stored = ArgumentCaptor.forClass(Feedback.class);
        verify(feedbacks).save(stored.capture());
        assertThat(stored.getValue()).extracting("member", "improvementSuggestions", "featureRequests", "status")
                .containsExactly(member, "Improve search", "New feature", FeedbackStatus.PENDING);
        var order = inOrder(features, notification);
        order.verify(features).saveAll(argThat(rows -> {
            var values = new java.util.ArrayList<FeedbackServiceFeature>();
            rows.forEach(values::add);
            assertThat(values).extracting("feedback").containsExactly(saved, saved);
            assertThat(values).extracting("serviceFeature").containsExactly(ServiceFeature.CHATBOT_CAMPUS_INFO, ServiceFeature.NOTICE_ALERT);
            return true;
        }));
        order.verify(notification).send("Improve search", 19L);
    }

    @Test
    void missingMemberSubmitsAnonymousFeedback() {
        when(members.getMemberReferenceByContext()).thenThrow(new MemberNotFoundException());
        when(feedbacks.save(any(Feedback.class))).thenReturn(Feedback.builder().id(20L).build());
        var request = new FeedbackCreate(List.of(ServiceFeature.MEAL_INFORMATION), "Improve meal", "Menu history");
        assertThat(service.submitFeedback(request)).isEqualTo(20L);
        verify(feedbacks).save(argThat(row -> {
            assertThat(row).extracting("member").isNull();
            return true;
        }));
        verify(notification).send("Improve meal", 20L);
    }

    @Test
    void storageFailureCannotPersistFeaturesOrNotify() {
        when(feedbacks.save(any(Feedback.class))).thenThrow(new IllegalStateException("storage unavailable"));
        var request = new FeedbackCreate(List.of(ServiceFeature.NOTICE_ALERT), "Improve notice", "New feature");
        assertThatThrownBy(() -> service.submitFeedback(request)).isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(features, notification);
    }

    @Test
    void featureStorageFailureCannotSendSuccessNotification() {
        when(feedbacks.save(any(Feedback.class))).thenReturn(Feedback.builder().id(20L).build());
        doThrow(new IllegalStateException("feature storage unavailable")).when(features).saveAll(any());
        var request = new FeedbackCreate(List.of(ServiceFeature.NOTICE_ALERT), "Improve notice", "New feature");
        assertThatThrownBy(() -> service.submitFeedback(request)).isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(notification);
    }

    @Test
    void queryFunctionsPreserveRepositoryResultsAndPage() {
        var detail = new FeedbackDetail(19L, "Improve", "Feature", 7L, "Member", List.of(), LocalDateTime.of(2026, 10, 16, 12, 0));
        var overview = new FeedbackOverview(List.of(), List.of("Improve"), List.of("Feature"));
        var page = PageRequest.of(2, 5);
        when(feedbacks.searchFeedbackById(19L)).thenReturn(Optional.of(detail));
        when(feedbacks.searchFeedbackOverview()).thenReturn(overview);
        when(feedbacks.searchAllImprovementSuggestions(page)).thenReturn(List.of("Improve"));
        when(feedbacks.searchAllFeatureRequests(page)).thenReturn(List.of("Feature"));
        assertThat(service.getFeedbackDetail(19L)).isSameAs(detail);
        assertThat(service.getFeedbackOverview()).isSameAs(overview);
        assertThat(service.getImprovementSuggestions(page)).containsExactly("Improve");
        assertThat(service.getFeatureRequests(page)).containsExactly("Feature");
    }

    @Test
    void absentFeedbackCannotReturnFabricatedDetail() {
        assertThatThrownBy(() -> service.getFeedbackDetail(999L)).isInstanceOf(FeedbackNotFoundException.class);
    }
}
