package com.dongsoop.dongsoop.projectBoard;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dongsoop.dongsoop.chat.service.ChatService;
import com.dongsoop.dongsoop.department.entity.Department;
import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.member.service.MemberService;
import com.dongsoop.dongsoop.recruitment.apply.project.dto.ApplyProjectBoardRequest;
import com.dongsoop.dongsoop.recruitment.apply.project.entity.ProjectApply;
import com.dongsoop.dongsoop.recruitment.apply.project.notification.ProjectApplyNotification;
import com.dongsoop.dongsoop.recruitment.apply.project.repository.ProjectApplyRepository;
import com.dongsoop.dongsoop.recruitment.apply.project.repository.ProjectApplyRepositoryCustom;
import com.dongsoop.dongsoop.recruitment.apply.project.service.ProjectApplyServiceImpl;
import com.dongsoop.dongsoop.recruitment.board.project.entity.ProjectBoard;
import com.dongsoop.dongsoop.recruitment.board.project.entity.ProjectBoardDepartment;
import com.dongsoop.dongsoop.recruitment.board.project.entity.ProjectBoardDepartment.ProjectBoardDepartmentId;
import com.dongsoop.dongsoop.recruitment.board.project.exception.ProjectBoardDepartmentMismatchException;
import com.dongsoop.dongsoop.recruitment.board.project.repository.ProjectBoardDepartmentRepository;
import com.dongsoop.dongsoop.recruitment.board.project.repository.ProjectBoardRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProjectRecruitmentDepartmentEligibilityValidatorTest {

    private static final Long BOARD_ID = 1L;
    private static final Long REQUESTER_ID = 1L;
    private static final Long OWNER_ID = 2L;
    private static final String TEST_TITLE = "This is a test title";
    private static final String TEST_CONTENT = "This is a test title";

    @InjectMocks
    private ProjectApplyServiceImpl projectApplyService;

    @Mock
    private ProjectBoardRepository projectBoardRepository;

    @Mock
    private ProjectApplyRepository projectApplyRepository;

    @Mock
    private ProjectBoardDepartmentRepository projectBoardDepartmentRepository;

    @Mock
    private ProjectApplyRepositoryCustom projectApplyRepositoryCustom;

    @Mock
    private ChatService chatService;

    @Mock
    private ProjectApplyNotification projectApplyNotification;

    @Mock
    private MemberService memberService;

    @Test
    @DisplayName("게시판 학과와 회원 학과 불일치 시 ProjectBoardDepartmentMismatchException 발생")
    void should_Throw_Exception_If_Member_Department_Mismatch_Board() {
        // given
        Department boardDepartment = new Department(DepartmentType.DEPT_2001, null, null); // 게시판 요구 학과
        Department memberDepartment = new Department(DepartmentType.DEPT_3001, null, null); // 사용자 학과

        // Security Context 조회 시 학과가 DEPT_3001인 회원이 조회됨
        Member member = Member.builder()
                .id(REQUESTER_ID)
                .department(memberDepartment)
                .build();
        when(memberService.getMemberReferenceByContext())
                .thenReturn(member);

        when(projectApplyRepositoryCustom.existsByBoardIdAndMemberId(eq(BOARD_ID), eq(REQUESTER_ID))) // null은 회원 ID를 의미
                .thenReturn(false);

        // 게시판 조회 시 Id가 1인 게시판 조회
        ProjectBoard projectBoard = ProjectBoard.builder()
                .id(BOARD_ID)
                .build();
        when(projectBoardRepository.findById(eq(BOARD_ID)))
                .thenReturn(Optional.of(projectBoard));

        // Id가 1인 게시판의 학과 조회 시 DEPT_2001인 학과가 등록되어 있음
        ProjectBoardDepartment projectBoardDepartment = getProjectBoardDepartment(boardDepartment, projectBoard);
        when(projectBoardDepartmentRepository.findByProjectBoardId(BOARD_ID))
                .thenReturn(List.of(projectBoardDepartment));

        ApplyProjectBoardRequest request = new ApplyProjectBoardRequest(BOARD_ID, TEST_TITLE, TEST_CONTENT);

        // when, then
        assertThrows(ProjectBoardDepartmentMismatchException.class, () -> projectApplyService.apply(request));
    }

    @Test
    @DisplayName("게시판 학과와 회원 학과 일치 시 저장 및 예외없이 응답된다")
    void should_Response_Created_If_Member_Department_match_Board() {
        // given
        Long boardId = 1L;
        Department department = new Department(DepartmentType.DEPT_2001, null, null);

        // Security Context 조회 시 학과가 DEPT_3001인 회원이 조회됨
        Member member = Member.builder()
                .department(department)
                .build();
        Member author = getAuthor();
        when(memberService.getMemberReferenceByContext())
                .thenReturn(member);

        // 게시판 조회 시 Id가 1인 게시판 조회
        ProjectBoard projectBoard = ProjectBoard.builder()
                .id(boardId)
                .author(author)
                .build();

        when(projectBoardRepository.findById(eq(boardId)))
                .thenReturn(Optional.of(projectBoard));

        // Id가 1인 게시판의 학과 조회 시 DEPT_2001인 학과가 등록되어 있음
        ProjectBoardDepartment projectBoardDepartment = getProjectBoardDepartment(department, projectBoard);
        when(projectBoardDepartmentRepository.findByProjectBoardId(boardId))
                .thenReturn(List.of(projectBoardDepartment));

        ApplyProjectBoardRequest request = new ApplyProjectBoardRequest(boardId, TEST_TITLE, TEST_CONTENT);

        // when, then
        assertDoesNotThrow(() -> projectApplyService.apply(request));
        verify(projectApplyRepository, times(1))
                .save(any(ProjectApply.class));
    }

    /**
     * 입력받은 학과의 "모집 게시판 요구 학과" 객체를 만드는 메서드
     *
     * @param department 게시판에 등록된 학과
     * @return ProjectBoardDepartment 객체
     */
    private ProjectBoardDepartment getProjectBoardDepartment(Department department, ProjectBoard projectBoard) {
        ProjectBoardDepartmentId projectBoardDepartmentId = new ProjectBoardDepartmentId(
                projectBoard,
                department
        );
        return new ProjectBoardDepartment(projectBoardDepartmentId);
    }

    private Member getAuthor() {
        return Member.builder()
                .id(OWNER_ID)
                .build();
    }
}
