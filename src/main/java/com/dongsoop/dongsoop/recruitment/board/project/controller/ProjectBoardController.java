package com.dongsoop.dongsoop.recruitment.board.project.controller;

import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.recruitment.board.dto.RecruitmentDetails;
import com.dongsoop.dongsoop.recruitment.board.dto.RecruitmentOverview;
import com.dongsoop.dongsoop.recruitment.board.project.dto.CreateProjectBoardRequest;
import com.dongsoop.dongsoop.recruitment.board.project.entity.ProjectBoard;
import com.dongsoop.dongsoop.recruitment.board.project.service.ProjectBoardService;
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
@RequestMapping("/project-board")
@RequiredArgsConstructor
public class ProjectBoardController {

    private final ProjectBoardService projectBoardService;

    @GetMapping("/{projectBoardId}")
    public ResponseEntity<RecruitmentDetails> getProjectBoardDetails(@PathVariable Long projectBoardId) {
        RecruitmentDetails details = projectBoardService.getBoardDetailsById(projectBoardId);

        return ResponseEntity.ok(details);
    }

    @PostMapping
    @Secured(value = RoleType.USER_ROLE)
    public ResponseEntity<Void> createProjectBoard(@RequestBody @Valid CreateProjectBoardRequest request) {
        ProjectBoard projectBoard = projectBoardService.create(request);

        URI uri = URI.create("/project-board/" + projectBoard.getId());

        return ResponseEntity.created(uri)
                .build();
    }

    @GetMapping
    public ResponseEntity<List<RecruitmentOverview>> getProjectBoardList(Pageable pageable) {
        List<RecruitmentOverview> overviews = projectBoardService.getBoardByPage(pageable);

        return ResponseEntity.ok(overviews);
    }

    @GetMapping("/department/{departmentType}")
    public ResponseEntity<List<RecruitmentOverview>> getProjectBoardListByDepartment(
            @PathVariable("departmentType") DepartmentType departmentType,
            Pageable pageable) {
        List<RecruitmentOverview> overviews = projectBoardService.getBoardByPageAndDepartmentType(
                departmentType,
                pageable);

        return ResponseEntity.ok(overviews);
    }

    @DeleteMapping("/{boardId}")
    public ResponseEntity<Void> deleteStudyBoard(@PathVariable("boardId") Long boardId) {
        projectBoardService.deleteBoardById(boardId);

        return ResponseEntity.noContent()
                .build();
    }
}
