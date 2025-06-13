package com.dongsoop.dongsoop.recruitment.study.service;

import com.dongsoop.dongsoop.department.entity.Department;
import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.department.repository.DepartmentRepository;
import com.dongsoop.dongsoop.exception.domain.member.MemberNotFoundException;
import com.dongsoop.dongsoop.exception.domain.study.StudyBoardNotFound;
import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.member.service.MemberService;
import com.dongsoop.dongsoop.recruitment.RecruitmentViewType;
import com.dongsoop.dongsoop.recruitment.study.dto.CreateStudyBoardRequest;
import com.dongsoop.dongsoop.recruitment.study.dto.StudyBoardDetails;
import com.dongsoop.dongsoop.recruitment.study.dto.StudyBoardOverview;
import com.dongsoop.dongsoop.recruitment.study.entity.StudyBoard;
import com.dongsoop.dongsoop.recruitment.study.entity.StudyBoardDepartment;
import com.dongsoop.dongsoop.recruitment.study.entity.StudyBoardDepartment.StudyBoardDepartmentId;
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

    public List<StudyBoardOverview> getBoardByPageAndDepartmentType(DepartmentType departmentType, Pageable pageable) {
        return studyBoardRepositoryCustom.findStudyBoardOverviewsByPageAndDepartmentType(departmentType, pageable);
    }

    public List<StudyBoardOverview> getBoardByPage(Pageable pageable) {
        return studyBoardRepositoryCustom.findStudyBoardOverviewsByPage(pageable);
    }

    public StudyBoardDetails getBoardDetailsById(Long boardId) {
        try {
            Member member = memberService.getMemberReferenceByContext();
            boolean isOwner = studyBoardRepository.existsByIdAndAuthor(boardId, member);
            if (isOwner) {
                return getBoardDetailsWithViewType(boardId, RecruitmentViewType.OWNER);
            }

            return getBoardDetailsWithViewType(boardId, RecruitmentViewType.MEMBER);
        } catch (MemberNotFoundException exception) {
            return getBoardDetailsWithViewType(boardId, RecruitmentViewType.GUEST);
        }
    }

    public StudyBoardDetails getBoardDetailsWithViewType(Long studyBoardId, RecruitmentViewType viewType) {
        return studyBoardRepositoryCustom.findBoardDetailsByIdAndViewType(studyBoardId, viewType)
                .orElseThrow(() -> new StudyBoardNotFound(studyBoardId));
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
