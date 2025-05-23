package com.dongsoop.dongsoop.study.service;

import com.dongsoop.dongsoop.department.entity.Department;
import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.department.repository.DepartmentRepository;
import com.dongsoop.dongsoop.exception.domain.study.StudyBoardNotFound;
import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.member.service.MemberService;
import com.dongsoop.dongsoop.study.dto.CreateStudyBoardRequest;
import com.dongsoop.dongsoop.study.dto.StudyBoardDetails;
import com.dongsoop.dongsoop.study.dto.StudyBoardOverview;
import com.dongsoop.dongsoop.study.entity.StudyBoard;
import com.dongsoop.dongsoop.study.entity.StudyBoardDepartment;
import com.dongsoop.dongsoop.study.entity.StudyBoardDepartment.StudyBoardDepartmentId;
import com.dongsoop.dongsoop.study.repository.StudyBoardDepartmentRepository;
import com.dongsoop.dongsoop.study.repository.StudyBoardRepository;
import com.dongsoop.dongsoop.study.repository.StudyBoardRepositoryCustom;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StudyBoardServiceImpl implements StudyBoardService {

    private final StudyBoardRepository studyBoardRepository;

    private final StudyBoardRepositoryCustom studyBoardRepositoryCustom;

    private final MemberService memberService;

    private final DepartmentRepository departmentRepository;

    private final StudyBoardDepartmentRepository studyBoardDepartmentRepository;

    public StudyBoard create(CreateStudyBoardRequest request) {
        StudyBoard studyBoardToSave = transformToStudyBoard(request);
        List<Department> departmentList = getDepartmentReferenceList(request.getDepartmentTypeList());

        StudyBoard studyBoard = studyBoardRepository.save(studyBoardToSave);
        List<StudyBoardDepartment> studyBoardDepartment = departmentList.stream()
                .map(department -> {
                    StudyBoardDepartmentId studyBoardDepartmentId = new StudyBoardDepartmentId(studyBoard, department);
                    return new StudyBoardDepartment(studyBoardDepartmentId);
                })
                .toList();

        studyBoardDepartmentRepository.saveAll(studyBoardDepartment);
        return studyBoard;
    }

    public List<StudyBoardOverview> getStudyBoardByPage(DepartmentType departmentType, Pageable pageable) {
        return studyBoardRepositoryCustom.findStudyBoardOverviewsByPage(departmentType, pageable);
    }

    public StudyBoardDetails getStudyBoardDetails(Long studyBoardId) {
        return studyBoardRepositoryCustom.findStudyBoardDetails(studyBoardId)
                .orElseThrow(() -> new StudyBoardNotFound(studyBoardId));
    }

    private StudyBoard transformToStudyBoard(CreateStudyBoardRequest request) {
        Member member = memberService.getMemberReferenceByContext();

        return StudyBoard.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .startAt(request.getStartAt())
                .endAt(request.getEndAt())
                .tags(request.getTags())
                .author(member)
                .build();
    }

    private List<Department> getDepartmentReferenceList(List<DepartmentType> departmentTypeList) {
        return departmentTypeList.stream()
                .map(departmentRepository::getReferenceById)
                .toList();
    }
}
