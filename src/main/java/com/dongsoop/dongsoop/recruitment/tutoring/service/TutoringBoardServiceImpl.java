package com.dongsoop.dongsoop.recruitment.tutoring.service;

import com.dongsoop.dongsoop.common.exception.authentication.NotAuthenticationException;
import com.dongsoop.dongsoop.department.entity.Department;
import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.department.repository.DepartmentRepository;
import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.member.service.MemberService;
import com.dongsoop.dongsoop.recruitment.RecruitmentViewType;
import com.dongsoop.dongsoop.recruitment.dto.RecruitmentDetails;
import com.dongsoop.dongsoop.recruitment.dto.RecruitmentOverview;
import com.dongsoop.dongsoop.recruitment.tutoring.dto.CreateTutoringBoardRequest;
import com.dongsoop.dongsoop.recruitment.tutoring.entity.TutoringBoard;
import com.dongsoop.dongsoop.recruitment.tutoring.exception.TutoringBoardNotFound;
import com.dongsoop.dongsoop.recruitment.tutoring.repository.TutoringApplyRepositoryCustom;
import com.dongsoop.dongsoop.recruitment.tutoring.repository.TutoringBoardRepository;
import com.dongsoop.dongsoop.recruitment.tutoring.repository.TutoringBoardRepositoryCustom;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TutoringBoardServiceImpl implements TutoringBoardService {

    private final TutoringBoardRepository tutoringBoardRepository;

    private final TutoringBoardRepositoryCustom tutoringBoardRepositoryCustom;

    private final TutoringApplyRepositoryCustom tutoringApplyRepositoryCustom;

    private final DepartmentRepository departmentRepository;

    private final MemberService memberService;

    public List<RecruitmentOverview> getBoardByPageAndDepartmentType(DepartmentType departmentType,
                                                                     Pageable pageable) {
        return tutoringBoardRepositoryCustom.findTutoringBoardOverviewsByPageAndDepartmentType(departmentType,
                pageable);
    }

    public List<RecruitmentOverview> getBoardByPage(Pageable pageable) {
        return tutoringBoardRepositoryCustom.findTutoringBoardOverviewsByPage(pageable);
    }

    public TutoringBoard create(CreateTutoringBoardRequest request) {
        TutoringBoard tutoringBoard = transformToTutoringBoard(request);
        return tutoringBoardRepository.save(tutoringBoard);
    }

    public RecruitmentDetails getBoardDetailsById(Long boardId) {
        try {
            Long memberId = memberService.getMemberIdByAuthentication();
            boolean isOwner = tutoringBoardRepository.existsByIdAndAuthorId(boardId, memberId);
            if (isOwner) {
                return getBoardDetailsWithViewType(boardId, RecruitmentViewType.OWNER);
            }

            boolean isAlreadyApplied = tutoringApplyRepositoryCustom.existsByBoardIdAndMemberId(boardId, memberId);

            return getBoardDetailsWithViewType(boardId, RecruitmentViewType.MEMBER, isAlreadyApplied);
        } catch (NotAuthenticationException exception) {
            return getBoardDetailsWithViewType(boardId, RecruitmentViewType.GUEST);
        }
    }

    private RecruitmentDetails getBoardDetailsWithViewType(Long boardId, RecruitmentViewType viewType) {
        return getBoardDetailsWithViewType(boardId, viewType, false);
    }

    private RecruitmentDetails getBoardDetailsWithViewType(Long boardId, RecruitmentViewType viewType,
                                                           boolean isAlreadyApplied) {
        return tutoringBoardRepositoryCustom.findBoardDetailsByIdAndViewType(boardId, viewType, isAlreadyApplied)
                .orElseThrow(() -> new TutoringBoardNotFound(boardId));
    }

    private TutoringBoard transformToTutoringBoard(CreateTutoringBoardRequest request) {
        Member memberReference = memberService.getMemberReferenceByContext();

        List<DepartmentType> departmentTypeList = request.departmentTypeList();
        Department departmentReference = departmentRepository.getReferenceById(departmentTypeList.get(0));

        return TutoringBoard.builder()
                .title(request.title())
                .content(request.content())
                .author(memberReference)
                .tags(request.tags())
                .department(departmentReference)
                .startAt(request.startAt())
                .endAt(request.endAt())
                .build();
    }
}
