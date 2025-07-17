package com.dongsoop.dongsoop.recruitment.board.study.controller;

import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.recruitment.board.dto.RecruitmentDetails;
import com.dongsoop.dongsoop.recruitment.board.dto.RecruitmentOverview;
import com.dongsoop.dongsoop.recruitment.board.study.dto.CreateStudyBoardRequest;
import com.dongsoop.dongsoop.recruitment.board.study.entity.StudyBoard;
import com.dongsoop.dongsoop.recruitment.board.study.service.StudyBoardService;
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
@RequestMapping("/study-board")
@RequiredArgsConstructor
public class StudyBoardController {

    private final StudyBoardService studyBoardService;

    @GetMapping("/{studyBoardId}")
    public ResponseEntity<RecruitmentDetails> getStudyBoardDetails(@PathVariable Long studyBoardId) {
        RecruitmentDetails details = studyBoardService.getBoardDetailsById(studyBoardId);

        return ResponseEntity.ok(details);
    }

    @PostMapping
    @Secured(value = RoleType.USER_ROLE)
    public ResponseEntity<Void> createStudyBoard(@RequestBody @Valid CreateStudyBoardRequest request) {
        StudyBoard studyBoard = studyBoardService.create(request);

        URI uri = URI.create("/study-board/" + studyBoard.getId());

        return ResponseEntity.created(uri)
                .build();
    }

    @GetMapping("/department/{departmentType}")
    public ResponseEntity<List<RecruitmentOverview>> getStudyBoardListByDepartmentType(
            @PathVariable("departmentType") DepartmentType departmentType,
            Pageable pageable) {
        List<RecruitmentOverview> overviews = studyBoardService.getBoardByPageAndDepartmentType(
                departmentType,
                pageable);

        return ResponseEntity.ok(overviews);
    }

    @GetMapping
    public ResponseEntity<List<RecruitmentOverview>> getStudyBoardList(Pageable pageable) {
        List<RecruitmentOverview> overviews = studyBoardService.getBoardByPage(pageable);

        return ResponseEntity.ok(overviews);
    }

    @DeleteMapping("/{boardId}")
    public ResponseEntity<Void> deleteStudyBoard(@PathVariable("boardId") Long boardId) {
        studyBoardService.deleteBoardById(boardId);

        return ResponseEntity.noContent()
                .build();
    }
}
