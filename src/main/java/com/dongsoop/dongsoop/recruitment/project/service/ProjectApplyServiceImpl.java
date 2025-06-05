package com.dongsoop.dongsoop.recruitment.project.service;

import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.member.service.MemberService;
import com.dongsoop.dongsoop.recruitment.project.dto.ApplyProjectBoardRequest;
import com.dongsoop.dongsoop.recruitment.project.entity.ProjectBoard;
import com.dongsoop.dongsoop.recruitment.project.entity.ProjectBoardApply;
import com.dongsoop.dongsoop.recruitment.project.entity.ProjectBoardApply.ProjectBoardApplyKey;
import com.dongsoop.dongsoop.recruitment.project.repository.ProjectApplyRepository;
import com.dongsoop.dongsoop.recruitment.project.repository.ProjectBoardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProjectApplyServiceImpl implements ProjectApplyService {

    private final MemberService memberService;

    private final ProjectApplyRepository projectApplyRepository;

    private final ProjectBoardRepository projectBoardRepository;

    public void apply(ApplyProjectBoardRequest request) {
        Member member = memberService.getMemberReferenceByContext();
        ProjectBoard referenceById = projectBoardRepository.getReferenceById(request.boardId());

        ProjectBoardApplyKey key = new ProjectBoardApplyKey(referenceById, member);

        ProjectBoardApply boardApply = ProjectBoardApply.builder()
                .id(key)
                .introduction(request.introduction())
                .motivation(request.motivation())
                .build();

        projectApplyRepository.save(boardApply);
    }
}
