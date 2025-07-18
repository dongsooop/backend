package com.dongsoop.dongsoop.recruitment.board.tutoring.controller;

import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.recruitment.board.dto.RecruitmentDetails;
import com.dongsoop.dongsoop.recruitment.board.dto.RecruitmentOverview;
import com.dongsoop.dongsoop.recruitment.board.tutoring.dto.CreateTutoringBoardRequest;
import com.dongsoop.dongsoop.recruitment.board.tutoring.entity.TutoringBoard;
import com.dongsoop.dongsoop.recruitment.board.tutoring.service.TutoringBoardService;
import com.dongsoop.dongsoop.role.entity.RoleType;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tutoring-board")
@RequiredArgsConstructor
public class TutoringBoardController {

    private final TutoringBoardService tutoringBoardService;

    @GetMapping("/department/{departmentType}")
    public ResponseEntity<List<RecruitmentOverview>> getTutoringBoardOverviewsByDepartment(
            @PathVariable("departmentType") DepartmentType departmentType, Pageable pageable) {
        List<RecruitmentOverview> tutoringBoardList = tutoringBoardService.getBoardByPageAndDepartmentType(
                departmentType,
                pageable);
        return ResponseEntity.ok(tutoringBoardList);
    }

    @GetMapping
    public ResponseEntity<List<RecruitmentOverview>> getTutoringBoardOverviews(Pageable pageable) {
        List<RecruitmentOverview> tutoringBoardList = tutoringBoardService.getBoardByPage(pageable);
        return ResponseEntity.ok(tutoringBoardList);
    }

    @GetMapping("/{boardId}")
    public ResponseEntity<RecruitmentDetails> getTutoringBoard(
            @PathVariable("boardId") Long boardId) {
        RecruitmentDetails tutoringBoard = tutoringBoardService.getBoardDetailsById(boardId);
        return ResponseEntity.ok(tutoringBoard);
    }

    @PostMapping
    @Secured(value = RoleType.USER_ROLE)
    public ResponseEntity<Void> createTutoringBoard(@Valid @RequestBody CreateTutoringBoardRequest request) {
        TutoringBoard createdBoard = tutoringBoardService.create(request);
        URI uri = URI.create("/tutoring-board/" + createdBoard.getId());

        return ResponseEntity.created(uri)
                .build();
    }

    @DeleteMapping("/{boardId}")
    @Secured(value = RoleType.USER_ROLE)
    public ResponseEntity<Void> deleteStudyBoard(@PathVariable("boardId") Long boardId) {
        tutoringBoardService.deleteBoardById(boardId);

        return ResponseEntity.noContent()
                .build();
    }
}
