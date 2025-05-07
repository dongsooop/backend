package com.dongsoop.dongsoop.tutoring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dongsoop.dongsoop.department.entity.Department;
import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.department.repository.DepartmentRepository;
import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.member.service.MemberService;
import com.dongsoop.dongsoop.tutoring.dto.CreateTutoringBoardRequest;
import com.dongsoop.dongsoop.tutoring.entity.TutoringBoard;
import com.dongsoop.dongsoop.tutoring.repository.TutoringBoardRepository;
import com.dongsoop.dongsoop.tutoring.service.TutoringBoardServiceImpl;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TutoringBoardCreateTest {

    private final String VALID_TITLE = "This is a test title";
    private final String VALID_CONTENT = "This is a test content";
    private final String VALID_TAGS = "tag1, tag2";
    private final Integer VALID_RECRUITMENT_CAPACITY = 5;
    private final DepartmentType VALID_DEPARTMENT_TYPE = DepartmentType.DEPT_2001;
    private final LocalDateTime VALID_START_AT = LocalDateTime.of(2099, 1, 1, 0, 0);
    private final LocalDateTime VALID_END_AT = LocalDateTime.of(2099, 12, 31, 23, 59);

    @InjectMocks
    private TutoringBoardServiceImpl tutoringBoardService;

    @Mock
    private MemberService memberService;

    @Mock
    private TutoringBoardRepository tutoringBoardRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    private CreateTutoringBoardRequest request;

    @BeforeEach
    void setUp() {
        request = CreateTutoringBoardRequest.builder()
                .title(VALID_TITLE)
                .content(VALID_CONTENT)
                .tags(VALID_TAGS)
                .recruitmentCapacity(VALID_RECRUITMENT_CAPACITY)
                .startAt(VALID_START_AT)
                .endAt(VALID_END_AT)
                .departmentType(VALID_DEPARTMENT_TYPE)
                .build();
    }

    @Test
    void create_WithValidInput_ShouldPersistCorrectly() {
        // given
        when(memberService.getMemberReferenceByContext())
                .thenReturn(
                        Member.builder()
                                .id(1L)
                                .build());

        when(tutoringBoardRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(departmentRepository.getReferenceById(VALID_DEPARTMENT_TYPE))
                .thenReturn(new Department(VALID_DEPARTMENT_TYPE, null, null));

        // when
        tutoringBoardService.create(this.request);

        // then
        ArgumentCaptor<TutoringBoard> captor = ArgumentCaptor.forClass(TutoringBoard.class);
        verify(tutoringBoardRepository).save(captor.capture());
        TutoringBoard board = captor.getValue();

        Department department = new Department(VALID_DEPARTMENT_TYPE, null, null);
        TutoringBoard compareBoard = TutoringBoard.builder()
                .title(VALID_TITLE)
                .content(VALID_CONTENT)
                .tags(VALID_TAGS)
                .capacity(VALID_RECRUITMENT_CAPACITY)
                .startAt(VALID_START_AT)
                .endAt(VALID_END_AT)
                .department(department)
                .author(Member.builder().id(1L).build())
                .build();

        assertThat(board)
                .usingRecursiveComparison()
                .ignoringExpectedNullFields()
                .ignoringFields("id", "boardDate.createdAt", "boardDate.updatedAt")
                .isEqualTo(compareBoard);
    }
}
