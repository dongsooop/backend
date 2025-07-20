package com.dongsoop.dongsoop.recruitment.board.project.service;

import com.dongsoop.dongsoop.chat.entity.ChatRoom;
import com.dongsoop.dongsoop.chat.service.ChatService;
import com.dongsoop.dongsoop.common.exception.authentication.NotAuthenticationException;
import com.dongsoop.dongsoop.department.entity.Department;
import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.department.repository.DepartmentRepository;
import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.member.service.MemberService;
import com.dongsoop.dongsoop.recruitment.RecruitmentViewType;
import com.dongsoop.dongsoop.recruitment.apply.project.repository.ProjectApplyRepositoryCustom;
import com.dongsoop.dongsoop.recruitment.board.dto.RecruitmentDetails;
import com.dongsoop.dongsoop.recruitment.board.dto.RecruitmentOverview;
import com.dongsoop.dongsoop.recruitment.board.project.dto.CreateProjectBoardRequest;
import com.dongsoop.dongsoop.recruitment.board.project.entity.ProjectBoard;
import com.dongsoop.dongsoop.recruitment.board.project.entity.ProjectBoardDepartment;
import com.dongsoop.dongsoop.recruitment.board.project.entity.ProjectBoardDepartment.ProjectBoardDepartmentId;
import com.dongsoop.dongsoop.recruitment.board.project.exception.ProjectBoardNotFound;
import com.dongsoop.dongsoop.recruitment.board.project.repository.ProjectBoardDepartmentRepository;
import com.dongsoop.dongsoop.recruitment.board.project.repository.ProjectBoardRepository;
import com.dongsoop.dongsoop.recruitment.board.project.repository.ProjectBoardRepositoryCustom;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProjectBoardServiceImpl implements ProjectBoardService {

    private final ProjectBoardRepository projectBoardRepository;

    private final ProjectBoardRepositoryCustom projectBoardRepositoryCustom;

    private final MemberService memberService;

    private final DepartmentRepository departmentRepository;

    private final ProjectBoardDepartmentRepository projectBoardDepartmentRepository;

    private final ProjectApplyRepositoryCustom projectApplyRepositoryCustom;

    private final ChatService chatService;

    @Transactional
    public ProjectBoard create(CreateProjectBoardRequest request) {
        ProjectBoard projectBoardToSave = transformToProjectBoard(request);
        List<Department> departmentList = getDepartmentReferenceList(request.departmentTypeList());

        ProjectBoard projectBoard = projectBoardRepository.save(projectBoardToSave);
        List<ProjectBoardDepartment> projectBoardDepartmentList = departmentList.stream()
                .map(department -> {
                    ProjectBoardDepartmentId projectBoardDepartmentId = new ProjectBoardDepartmentId(projectBoard,
                            department);
                    return new ProjectBoardDepartment(projectBoardDepartmentId);
                })
                .toList();

        projectBoardDepartmentRepository.saveAll(projectBoardDepartmentList);

        createAndLinkChatRoom(projectBoard);

        return projectBoard;
    }

    private void createAndLinkChatRoom(ProjectBoard projectBoard) {
        Member author = projectBoard.getAuthor();
        String chatRoomTitle = String.format("[프로젝트] %s", projectBoard.getTitle());

        Set<Long> initialParticipants = Set.of(author.getId());

        ChatRoom chatRoom = chatService.createGroupChatRoom(author.getId(), initialParticipants, chatRoomTitle);

        projectBoard.assignChatRoom(chatRoom.getRoomId());
        projectBoardRepository.save(projectBoard);
    }

    public List<RecruitmentOverview> getBoardByPageAndDepartmentType(DepartmentType departmentType,
                                                                     Pageable pageable) {
        return projectBoardRepositoryCustom.findProjectBoardOverviewsByPageAndDepartmentType(departmentType, pageable);
    }

    public List<RecruitmentOverview> getBoardByPage(Pageable pageable) {
        return projectBoardRepositoryCustom.findProjectBoardOverviewsByPage(pageable);
    }

    public RecruitmentDetails getBoardDetailsById(Long boardId) {
        try {
            Long memberId = memberService.getMemberIdByAuthentication();
            boolean isOwner = projectBoardRepository.existsByIdAndAuthorId(boardId, memberId);
            if (isOwner) {
                return getBoardDetailsWithViewType(boardId, RecruitmentViewType.OWNER);
            }

            boolean isAlreadyApplied = projectApplyRepositoryCustom.existsByBoardIdAndMemberId(boardId, memberId);

            return getBoardDetailsWithViewType(boardId, RecruitmentViewType.MEMBER, isAlreadyApplied);
        } catch (NotAuthenticationException exception) {
            return getBoardDetailsWithViewType(boardId, RecruitmentViewType.GUEST);
        }
    }

    private RecruitmentDetails getBoardDetailsWithViewType(Long boardId, RecruitmentViewType viewType,
                                                           boolean isAlreadyApplied) {
        return projectBoardRepositoryCustom.findBoardDetailsByIdAndViewType(boardId, viewType, isAlreadyApplied)
                .orElseThrow(() -> new ProjectBoardNotFound(boardId));
    }

    private RecruitmentDetails getBoardDetailsWithViewType(Long boardId, RecruitmentViewType viewType) {
        return getBoardDetailsWithViewType(boardId, viewType, false);
    }

    private ProjectBoard transformToProjectBoard(CreateProjectBoardRequest request) {
        Member member = memberService.getMemberReferenceByContext();

        return ProjectBoard.builder()
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

    @Override
    public void deleteBoardById(Long boardId) {
        Long requesterId = memberService.getMemberIdByAuthentication();
        if (!projectBoardRepository.existsByIdAndAuthorId(boardId, requesterId)) {
            throw new ProjectBoardNotFound(boardId, requesterId);
        }

        projectBoardRepository.deleteById(boardId);
    }
}
