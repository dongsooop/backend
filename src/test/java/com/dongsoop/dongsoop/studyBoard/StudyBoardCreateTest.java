package com.dongsoop.dongsoop.studyBoard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dongsoop.dongsoop.department.entity.Department;
import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.department.repository.DepartmentRepository;
import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.member.service.MemberService;
import com.dongsoop.dongsoop.recruitment.board.study.dto.CreateStudyBoardRequest;
import com.dongsoop.dongsoop.recruitment.board.study.entity.StudyBoard;
import com.dongsoop.dongsoop.recruitment.board.study.entity.StudyBoardDepartment;
import com.dongsoop.dongsoop.recruitment.board.study.repository.StudyBoardDepartmentRepository;
import com.dongsoop.dongsoop.recruitment.board.study.repository.StudyBoardRepository;
import com.dongsoop.dongsoop.recruitment.board.study.service.StudyBoardServiceImpl;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StudyBoardCreateTest {

    private final String VALID_TITLE = "This is a test title";
    private final String VALID_CONTENT = "This is a test content";
    private final String VALID_TAGS = "tag1, tag2";
    private final DepartmentType VALID_DEPARTMENT_TYPE_A = DepartmentType.DEPT_2001;
    private final DepartmentType VALID_DEPARTMENT_TYPE_B = DepartmentType.DEPT_3001;
    private final LocalDateTime VALID_START_AT = LocalDateTime.of(2099, 1, 1, 0, 0);
    private final LocalDateTime VALID_END_AT = LocalDateTime.of(2099, 12, 31, 23, 59);

    @InjectMocks
    private StudyBoardServiceImpl studyBoardService;

    @Mock
    private MemberService memberService;

    @Mock
    private StudyBoardRepository studyBoardRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private StudyBoardDepartmentRepository studyBoardDepartmentRepository;

    private CreateStudyBoardRequest request;

    @BeforeEach
    void setUp() {
        List<DepartmentType> departmentList = new ArrayList<>();
        departmentList.add(VALID_DEPARTMENT_TYPE_A);
        departmentList.add(VALID_DEPARTMENT_TYPE_B);

        request = new CreateStudyBoardRequest(
                VALID_TITLE,
                VALID_CONTENT,
                VALID_TAGS,
                VALID_START_AT,
                VALID_END_AT,
                departmentList
        );
    }

    @Test
    void create_WithValidInput_ShouldPersistCorrectly() {
        // given
        when(memberService.getMemberReferenceByContext())
                .thenReturn(
                        Member.builder()
                                .id(1L)
                                .build());

        when(studyBoardRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(departmentRepository.getReferenceById(VALID_DEPARTMENT_TYPE_A))
                .thenReturn(new Department(VALID_DEPARTMENT_TYPE_A, null, null));

        when(departmentRepository.getReferenceById(VALID_DEPARTMENT_TYPE_B))
                .thenReturn(new Department(VALID_DEPARTMENT_TYPE_B, null, null));

        // when
        studyBoardService.create(this.request);

        // then
        ArgumentCaptor<StudyBoard> captorStudyBoard = ArgumentCaptor.forClass(StudyBoard.class);
        ArgumentCaptor<List<StudyBoardDepartment>> captorDepartment = ArgumentCaptor.forClass(List.class);

        verify(studyBoardRepository).save(captorStudyBoard.capture());
        verify(studyBoardDepartmentRepository).saveAll(captorDepartment.capture());

        StudyBoard board = captorStudyBoard.getValue();
        List<StudyBoardDepartment> studyBoardDepartmentList = captorDepartment.getValue();

        StudyBoard compareBoard = StudyBoard.builder()
                .title(VALID_TITLE)
                .content(VALID_CONTENT)
                .tags(VALID_TAGS)
                .startAt(VALID_START_AT)
                .endAt(VALID_END_AT)
                .author(Member.builder().id(1L).build())
                .build();

        List<StudyBoardDepartment> compareDepartmentList = List.of(
                new StudyBoardDepartment(new StudyBoardDepartment.StudyBoardDepartmentId(board,
                        new Department(VALID_DEPARTMENT_TYPE_A, null, null))),
                new StudyBoardDepartment(new StudyBoardDepartment.StudyBoardDepartmentId(board,
                        new Department(VALID_DEPARTMENT_TYPE_B, null, null)))
        );

        assertThat(board)
                .usingRecursiveComparison()
                .ignoringExpectedNullFields()
                .ignoringFields("id", "createdAt", "updatedAt")
                .isEqualTo(compareBoard);

        assertThat(studyBoardDepartmentList)
                .usingRecursiveComparison()
                .ignoringExpectedNullFields()
                .isEqualTo(compareDepartmentList);
    }
}
