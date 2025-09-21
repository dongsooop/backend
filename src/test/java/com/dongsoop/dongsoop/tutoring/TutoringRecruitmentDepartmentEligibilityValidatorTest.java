package com.dongsoop.dongsoop.tutoring;

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
import com.dongsoop.dongsoop.recruitment.apply.tutoring.dto.ApplyTutoringBoardRequest;
import com.dongsoop.dongsoop.recruitment.apply.tutoring.entity.TutoringApply;
import com.dongsoop.dongsoop.recruitment.apply.tutoring.notification.TutoringApplyNotification;
import com.dongsoop.dongsoop.recruitment.apply.tutoring.repository.TutoringApplyRepository;
import com.dongsoop.dongsoop.recruitment.apply.tutoring.repository.TutoringApplyRepositoryCustom;
import com.dongsoop.dongsoop.recruitment.apply.tutoring.service.TutoringApplyServiceImpl;
import com.dongsoop.dongsoop.recruitment.board.tutoring.entity.TutoringBoard;
import com.dongsoop.dongsoop.recruitment.board.tutoring.exception.TutoringBoardDepartmentMismatchException;
import com.dongsoop.dongsoop.recruitment.board.tutoring.repository.TutoringBoardRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TutoringRecruitmentDepartmentEligibilityValidatorTest {

    @InjectMocks
    private TutoringApplyServiceImpl tutoringApplyService;

    @Mock
    private TutoringBoardRepository tutoringBoardRepository;

    @Mock
    private TutoringApplyRepository tutoringApplyRepository;

    @Mock
    private TutoringApplyRepositoryCustom tutoringApplyRepositoryCustom;

    @Mock
    private ChatService chatService;

    @Mock
    private TutoringApplyNotification tutoringApplyNotification;

    @Mock
    private MemberService memberService;

    @Test
    @DisplayName("게시판 학과와 회원 학과 불일치 시 TutoringBoardDepartmentMismatchException 발생")
    void should_Throw_Exception_If_MemberDepartment_Mismatch_Board() {
        // given
        Long boardId = 1L;
        Long memberId = 1L;
        Department boardDepartment = new Department(DepartmentType.DEPT_2001, null, null); // 게시판 요구 학과
        Department memberDepartment = new Department(DepartmentType.DEPT_3001, null, null); // 사용자 학과

        // Security Context 조회 시 학과가 DEPT_3001인 회원이 조회됨
        Member member = Member.builder()
                .id(memberId)
                .department(memberDepartment)
                .build();
        Member author = Member.builder()
                .id(0L)
                .build();
        when(memberService.getMemberReferenceByContext())
                .thenReturn(member);

        when(tutoringApplyRepositoryCustom.existsByBoardIdAndMemberId(eq(boardId), eq(memberId))) // null은 회원 ID를 의미
                .thenReturn(false);

        // 게시판 조회 시 Id가 1인 게시판 조회
        TutoringBoard tutoringBoard = TutoringBoard.builder()
                .id(boardId)
                .department(boardDepartment)
                .author(author)
                .build();
        when(tutoringBoardRepository.findById(eq(boardId)))
                .thenReturn(Optional.of(tutoringBoard));

        ApplyTutoringBoardRequest request = new ApplyTutoringBoardRequest(boardId, "소개글", "지원동기");

        // when, then
        assertThrows(TutoringBoardDepartmentMismatchException.class, () -> tutoringApplyService.apply(request));
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
        Member author = Member.builder()
                .id(0L)
                .build();
        when(memberService.getMemberReferenceByContext())
                .thenReturn(member);

        // 게시판 조회 시 Id가 1인 게시판 조회
        TutoringBoard tutoringBoard = TutoringBoard.builder()
                .id(boardId)
                .department(department)
                .author(author)
                .build();
        when(tutoringBoardRepository.findById(eq(boardId)))
                .thenReturn(Optional.of(tutoringBoard));

        ApplyTutoringBoardRequest request = new ApplyTutoringBoardRequest(boardId, "소개글", "지원동기");

        // when, then
        assertDoesNotThrow(() -> tutoringApplyService.apply(request));
        verify(tutoringApplyRepository, times(1))
                .save(any(TutoringApply.class));
    }
}
