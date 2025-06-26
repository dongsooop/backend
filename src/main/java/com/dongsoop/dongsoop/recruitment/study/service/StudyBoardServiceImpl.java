package com.dongsoop.dongsoop.recruitment.study.service;

import com.dongsoop.dongsoop.department.entity.Department;
import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.department.repository.DepartmentRepository;
import com.dongsoop.dongsoop.exception.domain.authentication.NotAuthenticationException;
import com.dongsoop.dongsoop.exception.domain.study.StudyBoardNotFound;
import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.member.service.MemberService;
import com.dongsoop.dongsoop.recruitment.RecruitmentViewType;
import com.dongsoop.dongsoop.recruitment.dto.RecruitmentDetails;
import com.dongsoop.dongsoop.recruitment.dto.RecruitmentOverview;
import com.dongsoop.dongsoop.recruitment.study.dto.CreateStudyBoardRequest;
import com.dongsoop.dongsoop.recruitment.study.entity.StudyBoard;
import com.dongsoop.dongsoop.recruitment.study.entity.StudyBoardDepartment;
import com.dongsoop.dongsoop.recruitment.study.entity.StudyBoardDepartment.StudyBoardDepartmentId;
import com.dongsoop.dongsoop.recruitment.study.repository.StudyApplyRepositoryCustom;
import com.dongsoop.dongsoop.recruitment.study.repository.StudyBoardDepartmentRepository;
import com.dongsoop.dongsoop.recruitment.study.repository.StudyBoardRepository;
import com.dongsoop.dongsoop.recruitment.study.repository.StudyBoardRepositoryCustom;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StudyBoardServiceImpl implements StudyBoardService {

    private final StudyBoardRepository studyBoardRepository;

    private final StudyBoardRepositoryCustom studyBoardRepositoryCustom;

    private final MemberService memberService;

    private final DepartmentRepository departmentRepository;

    private final StudyBoardDepartmentRepository studyBoardDepartmentRepository;

    private final StudyApplyRepositoryCustom studyApplyRepositoryCustom;

    @Transactional
    public StudyBoard create(CreateStudyBoardRequest request) {
        StudyBoard studyBoardToSave = transformToStudyBoard(request);
        List<Department> departmentList = getDepartmentReferenceList(request.departmentTypeList());

        StudyBoard studyBoard = studyBoardRepository.save(studyBoardToSave);
        List<StudyBoardDepartment> studyBoardDepartmentList = departmentList.stream()
                .map(department -> {
                    StudyBoardDepartmentId studyBoardDepartmentId = new StudyBoardDepartmentId(studyBoard, department);
                    return new StudyBoardDepartment(studyBoardDepartmentId);
                })
                .toList();

        studyBoardDepartmentRepository.saveAll(studyBoardDepartmentList);
        return studyBoard;
    }

    public List<RecruitmentOverview> getBoardByPageAndDepartmentType(DepartmentType departmentType, Pageable pageable) {
        return studyBoardRepositoryCustom.findStudyBoardOverviewsByPageAndDepartmentType(departmentType, pageable);
    }

    public List<RecruitmentOverview> getBoardByPage(Pageable pageable) {
        return studyBoardRepositoryCustom.findStudyBoardOverviewsByPage(pageable);
    }

    public RecruitmentDetails getBoardDetailsById(Long boardId) {
        try {
            Long memberId = memberService.getMemberIdByAuthentication();
            boolean isOwner = studyBoardRepository.existsByIdAndAuthorId(boardId, memberId);
            if (isOwner) {
                return getBoardDetailsWithViewType(boardId, RecruitmentViewType.OWNER);
            }

            boolean isAlreadyApplied = studyApplyRepositoryCustom.existsByBoardIdAndMemberId(boardId, memberId);

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
        return studyBoardRepositoryCustom.findBoardDetailsByIdAndViewType(boardId, viewType, isAlreadyApplied)
                .orElseThrow(() -> new StudyBoardNotFound(boardId));
    }

    private StudyBoard transformToStudyBoard(CreateStudyBoardRequest request) {
        Member member = memberService.getMemberReferenceByContext();

        return StudyBoard.builder()
                .title(request.title())
                .content(request.content())
                .startAt(request.startAt())
                .endAt(request.endAt())
                .tags(request.tags())
                .author(member)
                .build();
    }

    private List<Department> getDepartmentReferenceList(List<DepartmentType> departmentTypeList) {
        return departmentTypeList.stream()
                .map(departmentRepository::getReferenceById)
                .toList();
    }
}
