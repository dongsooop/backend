package com.dongsoop.dongsoop.recruitment.board.tutoring.service;

import com.dongsoop.dongsoop.chat.entity.ChatRoom;
import com.dongsoop.dongsoop.chat.service.ChatRoomService;
import com.dongsoop.dongsoop.chat.service.ChatService;
import com.dongsoop.dongsoop.common.exception.authentication.NotAuthenticationException;
import com.dongsoop.dongsoop.department.entity.Department;
import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.department.repository.DepartmentRepository;
import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.member.service.MemberService;
import com.dongsoop.dongsoop.recruitment.RecruitmentViewType;
import com.dongsoop.dongsoop.recruitment.apply.tutoring.repository.TutoringApplyRepositoryCustom;
import com.dongsoop.dongsoop.recruitment.board.dto.RecruitmentDetails;
import com.dongsoop.dongsoop.recruitment.board.dto.RecruitmentOverview;
import com.dongsoop.dongsoop.recruitment.board.tutoring.dto.CreateTutoringBoardRequest;
import com.dongsoop.dongsoop.recruitment.board.tutoring.entity.TutoringBoard;
import com.dongsoop.dongsoop.recruitment.board.tutoring.exception.TutoringBoardNotFound;
import com.dongsoop.dongsoop.recruitment.board.tutoring.repository.TutoringBoardRepository;
import com.dongsoop.dongsoop.recruitment.board.tutoring.repository.TutoringBoardRepositoryCustom;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TutoringBoardServiceImpl implements TutoringBoardService {

    private final TutoringBoardRepository tutoringBoardRepository;

    private final TutoringBoardRepositoryCustom tutoringBoardRepositoryCustom;

    private final TutoringApplyRepositoryCustom tutoringApplyRepositoryCustom;

    private final DepartmentRepository departmentRepository;

    private final MemberService memberService;

    private final ChatService chatService;
    private final ChatRoomService chatRoomService;

    public List<RecruitmentOverview> getBoardByPageAndDepartmentType(DepartmentType departmentType,
                                                                     Pageable pageable) {
        return tutoringBoardRepositoryCustom.findTutoringBoardOverviewsByPageAndDepartmentType(departmentType,
                pageable);
    }

    public List<RecruitmentOverview> getBoardByPage(Pageable pageable) {
        return tutoringBoardRepositoryCustom.findTutoringBoardOverviewsByPage(pageable);
    }

    @Transactional
    public TutoringBoard create(CreateTutoringBoardRequest request) {
        TutoringBoard tutoringBoard = transformToTutoringBoard(request);
        return createAndLinkChatRoom(tutoringBoard);
    }

    private TutoringBoard createAndLinkChatRoom(TutoringBoard tutoringBoard) {
        Member author = tutoringBoard.getAuthor();
        String chatRoomTitle = String.format("[튜터링] %s", tutoringBoard.getTitle());

        Set<Long> initialParticipants = Set.of(author.getId());

        ChatRoom chatRoom = chatRoomService.createGroupChatRoom(author.getId(), initialParticipants, chatRoomTitle);

        tutoringBoard.assignChatRoom(chatRoom.getRoomId());
        return tutoringBoardRepository.save(tutoringBoard);
    }

    public RecruitmentDetails getBoardDetailsById(Long boardId) {
        try {
            Long memberId = memberService.getMemberIdByAuthentication();
            boolean isOwner = tutoringBoardRepository.existsByIdAndAuthorId(boardId, memberId);
            if (isOwner) {
                return getBoardDetailsWithViewType(boardId, RecruitmentViewType.OWNER);
            }

            boolean isAlreadyApplied = tutoringApplyRepositoryCustom.existsByBoardIdAndMemberId(boardId, memberId);

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
        return tutoringBoardRepositoryCustom.findBoardDetailsByIdAndViewType(boardId, viewType, isAlreadyApplied)
                .orElseThrow(() -> new TutoringBoardNotFound(boardId));
    }

    private TutoringBoard transformToTutoringBoard(CreateTutoringBoardRequest request) {
        Member memberReference = memberService.getMemberReferenceByContext();

        List<DepartmentType> departmentTypeList = request.departmentTypeList();
        Department departmentReference = departmentRepository.getReferenceById(departmentTypeList.get(0));

        return TutoringBoard.builder()
                .title(request.title())
                .content(request.content())
                .author(memberReference)
                .tags(request.tags())
                .department(departmentReference)
                .startAt(request.startAt())
                .endAt(request.endAt())
                .build();
    }

    @Override
    public void deleteBoardById(Long boardId) {
        Long requesterId = memberService.getMemberIdByAuthentication();
        if (!tutoringBoardRepository.existsByIdAndAuthorId(boardId, requesterId)) {
            throw new TutoringBoardNotFound(boardId, requesterId);
        }

        tutoringBoardRepository.deleteById(boardId);
    }
}
