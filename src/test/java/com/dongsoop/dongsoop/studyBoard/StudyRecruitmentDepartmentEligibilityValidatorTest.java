package com.dongsoop.dongsoop.studyBoard;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.dongsoop.dongsoop.department.entity.Department;
import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.exception.domain.study.StudyBoardDepartmentMismatchException;
import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.member.service.MemberService;
import com.dongsoop.dongsoop.recruitment.study.dto.ApplyStudyBoardRequest;
import com.dongsoop.dongsoop.recruitment.study.entity.StudyBoard;
import com.dongsoop.dongsoop.recruitment.study.entity.StudyBoardDepartment;
import com.dongsoop.dongsoop.recruitment.study.entity.StudyBoardDepartment.StudyBoardDepartmentId;
import com.dongsoop.dongsoop.recruitment.study.repository.StudyBoardDepartmentRepository;
import com.dongsoop.dongsoop.recruitment.study.repository.StudyBoardRepository;
import com.dongsoop.dongsoop.recruitment.study.service.StudyApplyServiceImpl;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StudyRecruitmentDepartmentEligibilityValidatorTest {

    @InjectMocks
    private StudyApplyServiceImpl studyApplyService;

    @Mock
    private StudyBoardRepository studyBoardRepository;

    @Mock
    private StudyBoardDepartmentRepository studyBoardDepartmentRepository;

    @Mock
    private MemberService memberService;

    @Test
    @DisplayName("게시판 학과와 회원 학과 불일치 시 StudyBoardDepartmentMismatchException 발생")
    void should_Throw_Exception_If_MemberDepartment_Mismatch_Board() {
        // given
        Long boardId = 1L;
        Department boardDepartment = new Department(DepartmentType.DEPT_2001, null, null); // 게시판 요구 학과
        Department memberDepartment = new Department(DepartmentType.DEPT_3001, null, null); // 사용자 학과

        // Security Context 조회 시 학과가 DEPT_3001인 회원이 조회됨
        Member member = Member.builder()
                .department(memberDepartment)
                .build();
        when(memberService.getMemberReferenceByContext())
                .thenReturn(member);

        // 게시판 조회 시 Id가 1인 게시판 조회
        StudyBoard studyBoard = StudyBoard.builder()
                .id(boardId)
                .build();
        when(studyBoardRepository.findById(eq(boardId)))
                .thenReturn(Optional.of(studyBoard));

        // Id가 1인 게시판의 학과 조회 시 DEPT_2001인 학과가 등록되어 있음
        StudyBoardDepartment studyBoardDepartment = getStudyBoardDepartment(boardDepartment, studyBoard);
        when(studyBoardDepartmentRepository.findByStudyBoardId(boardId))
                .thenReturn(List.of(studyBoardDepartment));

        ApplyStudyBoardRequest request = new ApplyStudyBoardRequest(boardId, "소개글", "지원동기");

        // when, then
        assertThrows(StudyBoardDepartmentMismatchException.class, () -> {
            studyApplyService.apply(request);
        });
    }

    /**
     * 입력받은 학과의 "모집 게시판 요구 학과" 객체를 만드는 메서드
     *
     * @param department 게시판에 등록된 학과
     * @return StudyBoardDepartment 객체
     */
    private StudyBoardDepartment getStudyBoardDepartment(Department department, StudyBoard studyBoard) {
        StudyBoardDepartmentId studyBoardDepartmentId = new StudyBoardDepartmentId(
                studyBoard,
                department
        );
        return new StudyBoardDepartment(studyBoardDepartmentId);
    }
}
