package com.dongsoop.dongsoop.eclass.service;

import com.dongsoop.dongsoop.eclass.dto.EclassAssignmentItem;
import com.dongsoop.dongsoop.eclass.dto.EclassAssignmentResponse;
import com.dongsoop.dongsoop.eclass.dto.EclassAssignmentSaveRequest;
import com.dongsoop.dongsoop.eclass.entity.EclassAssignment;
import com.dongsoop.dongsoop.eclass.repository.EclassAssignmentRepository;
import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.member.exception.MemberNotFoundException;
import com.dongsoop.dongsoop.member.repository.MemberRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EclassAssignmentServiceImpl implements EclassAssignmentService {

    private final EclassAssignmentRepository eclassAssignmentRepository;
    private final MemberRepository memberRepository;

    @Override
    @Transactional
    public List<EclassAssignmentResponse> saveAssignments(Long memberId, EclassAssignmentSaveRequest request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(MemberNotFoundException::new);

        for (EclassAssignmentItem item : request.getAssignments()) {
            Optional<EclassAssignment> existing = eclassAssignmentRepository
                    .findByMemberIdAndEclassIdAndIsDeletedFalse(memberId, item.getEclassId());

            if (existing.isPresent()) {
                existing.get().update(item.getDueDate(), item.isSubmitted(), item.getStatus());
            } else {
                EclassAssignment assignment = EclassAssignment.builder()
                        .member(member)
                        .eclassId(item.getEclassId())
                        .courseId(item.getCourseId())
                        .courseName(item.getCourseName())
                        .title(item.getTitle())
                        .dueDate(item.getDueDate())
                        .isSubmitted(item.isSubmitted())
                        .status(item.getStatus())
                        .link(item.getLink())
                        .build();
                eclassAssignmentRepository.save(assignment);
            }
        }

        return getAssignments(memberId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EclassAssignmentResponse> getAssignments(Long memberId) {
        return eclassAssignmentRepository
                .findByMemberIdAndIsDeletedFalseOrderByCreatedAtDesc(memberId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private EclassAssignmentResponse toResponse(EclassAssignment assignment) {
        return new EclassAssignmentResponse(
                assignment.getId(),
                assignment.getEclassId(),
                assignment.getCourseId(),
                assignment.getCourseName(),
                assignment.getTitle(),
                assignment.getDueDate(),
                assignment.isSubmitted(),
                assignment.getStatus(),
                assignment.getLink(),
                assignment.getCreatedAt(),
                assignment.getUpdatedAt()
        );
    }
}
