package com.dongsoop.dongsoop.recruitment.tutoring.service;

import com.dongsoop.dongsoop.department.entity.Department;
import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.department.repository.DepartmentRepository;
import com.dongsoop.dongsoop.exception.domain.member.MemberNotFoundException;
import com.dongsoop.dongsoop.exception.domain.tutoring.TutoringBoardNotFound;
import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.member.service.MemberService;
import com.dongsoop.dongsoop.recruitment.RecruitmentViewType;
import com.dongsoop.dongsoop.recruitment.tutoring.dto.CreateTutoringBoardRequest;
import com.dongsoop.dongsoop.recruitment.tutoring.dto.TutoringBoardDetails;
import com.dongsoop.dongsoop.recruitment.tutoring.dto.TutoringBoardOverview;
import com.dongsoop.dongsoop.recruitment.tutoring.entity.TutoringBoard;
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

    private final DepartmentRepository departmentRepository;

    private final MemberService memberService;

    public List<TutoringBoardOverview> getBoardByPageAndDepartmentType(DepartmentType departmentType,
                                                                       Pageable pageable) {
        return tutoringBoardRepositoryCustom.findTutoringBoardOverviewsByPageAndDepartmentType(departmentType,
                pageable);
    }

    public List<TutoringBoardOverview> getBoardByPage(Pageable pageable) {
        return tutoringBoardRepositoryCustom.findTutoringBoardOverviewsByPage(pageable);
    }

    public TutoringBoard create(CreateTutoringBoardRequest request) {
        TutoringBoard tutoringBoard = transformToTutoringBoard(request);
        return tutoringBoardRepository.save(tutoringBoard);
    }

    public TutoringBoardDetails getBoardDetailsById(Long boardId) {
        try {
            Member member = memberService.getMemberReferenceByContext();
            boolean isOwner = tutoringBoardRepository.existsByIdAndAuthor(boardId, member);
            if (isOwner) {
                return getBoardDetailsWithViewType(boardId, RecruitmentViewType.OWNER);
            }

            return getBoardDetailsWithViewType(boardId, RecruitmentViewType.MEMBER);
        } catch (MemberNotFoundException exception) {
            return getBoardDetailsWithViewType(boardId, RecruitmentViewType.GUEST);
        }
    }

    private TutoringBoardDetails getBoardDetailsWithViewType(Long tutoringBoardId, RecruitmentViewType viewType) {
        return tutoringBoardRepositoryCustom.findBoardDetailsByIdAndViewType(tutoringBoardId, viewType)
                .orElseThrow(() -> new TutoringBoardNotFound(tutoringBoardId));
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
