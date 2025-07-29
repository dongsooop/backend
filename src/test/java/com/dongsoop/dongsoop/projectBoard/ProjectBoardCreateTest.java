package com.dongsoop.dongsoop.projectBoard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dongsoop.dongsoop.chat.entity.ChatRoom;
import com.dongsoop.dongsoop.chat.service.ChatService;
import com.dongsoop.dongsoop.department.entity.Department;
import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.department.repository.DepartmentRepository;
import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.member.service.MemberService;
import com.dongsoop.dongsoop.recruitment.board.project.dto.CreateProjectBoardRequest;
import com.dongsoop.dongsoop.recruitment.board.project.entity.ProjectBoard;
import com.dongsoop.dongsoop.recruitment.board.project.entity.ProjectBoardDepartment;
import com.dongsoop.dongsoop.recruitment.board.project.entity.ProjectBoardDepartment.ProjectBoardDepartmentId;
import com.dongsoop.dongsoop.recruitment.board.project.repository.ProjectBoardDepartmentRepository;
import com.dongsoop.dongsoop.recruitment.board.project.repository.ProjectBoardRepository;
import com.dongsoop.dongsoop.recruitment.board.project.service.ProjectBoardServiceImpl;
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
class ProjectBoardCreateTest {

    private final String VALID_TITLE = "This is a test title";
    private final String VALID_CONTENT = "This is a test content";
    private final String VALID_TAGS = "tag1, tag2";
    private final DepartmentType VALID_DEPARTMENT_TYPE_A = DepartmentType.DEPT_2001;
    private final DepartmentType VALID_DEPARTMENT_TYPE_B = DepartmentType.DEPT_3001;
    private final LocalDateTime VALID_START_AT = LocalDateTime.of(2099, 1, 1, 0, 0);
    private final LocalDateTime VALID_END_AT = LocalDateTime.of(2099, 12, 31, 23, 59);

    @InjectMocks
    private ProjectBoardServiceImpl projectBoardService;

    @Mock
    private MemberService memberService;

    @Mock
    private ProjectBoardRepository projectBoardRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private ProjectBoardDepartmentRepository projectBoardDepartmentRepository;

    @Mock
    private ChatService chatService;

    private CreateProjectBoardRequest request;

    @BeforeEach
    void setUp() {
        List<DepartmentType> departmentList = new ArrayList<>();
        departmentList.add(VALID_DEPARTMENT_TYPE_A);
        departmentList.add(VALID_DEPARTMENT_TYPE_B);

        request = new CreateProjectBoardRequest(
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

        when(chatService.createGroupChatRoom(any(), any(), any()))
                .thenReturn(
                        ChatRoom.builder()
                                .roomId("")
                                .build());

        when(projectBoardRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(departmentRepository.getReferenceById(VALID_DEPARTMENT_TYPE_A))
                .thenReturn(new Department(VALID_DEPARTMENT_TYPE_A, null, null));

        when(departmentRepository.getReferenceById(VALID_DEPARTMENT_TYPE_B))
                .thenReturn(new Department(VALID_DEPARTMENT_TYPE_B, null, null));

        // when
        projectBoardService.create(this.request);

        // then
        ArgumentCaptor<ProjectBoard> captorProjectBoard = ArgumentCaptor.forClass(ProjectBoard.class);
        ArgumentCaptor<List<ProjectBoardDepartment>> captorDepartment = ArgumentCaptor.forClass(List.class);

        verify(projectBoardRepository).save(captorProjectBoard.capture());
        verify(projectBoardDepartmentRepository).saveAll(captorDepartment.capture());

        ProjectBoard board = captorProjectBoard.getValue();
        List<ProjectBoardDepartment> projectBoardDepartmentList = captorDepartment.getValue();

        ProjectBoard compareBoard = ProjectBoard.builder()
                .title(VALID_TITLE)
                .content(VALID_CONTENT)
                .tags(VALID_TAGS)
                .startAt(VALID_START_AT)
                .endAt(VALID_END_AT)
                .author(Member.builder().id(1L).build())
                .build();

        List<ProjectBoardDepartment> compareDepartmentList = List.of(
                new ProjectBoardDepartment(new ProjectBoardDepartmentId(board,
                        new Department(VALID_DEPARTMENT_TYPE_A, null, null))),
                new ProjectBoardDepartment(new ProjectBoardDepartmentId(board,
                        new Department(VALID_DEPARTMENT_TYPE_B, null, null)))
        );

        assertThat(board)
                .usingRecursiveComparison()
                .ignoringExpectedNullFields()
                .ignoringFields("id", "createdAt", "updatedAt")
                .isEqualTo(compareBoard);

        assertThat(projectBoardDepartmentList)
                .usingRecursiveComparison()
                .ignoringExpectedNullFields()
                .isEqualTo(compareDepartmentList);
    }
}
