package com.dongsoop.dongsoop.recruitment.project.controller;

import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.recruitment.project.dto.CreateProjectBoardRequest;
import com.dongsoop.dongsoop.recruitment.project.dto.ProjectBoardDetails;
import com.dongsoop.dongsoop.recruitment.project.dto.ProjectBoardOverview;
import com.dongsoop.dongsoop.recruitment.project.entity.ProjectBoard;
import com.dongsoop.dongsoop.recruitment.project.service.ProjectBoardService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<ProjectBoardDetails> getProjectBoardDetails(@PathVariable Long projectBoardId) {
        ProjectBoardDetails projectBoardDetails = projectBoardService.getBoardDetailsById(projectBoardId);

        return ResponseEntity.ok(projectBoardDetails);
    }

    @PostMapping
    public ResponseEntity<Void> createProjectBoard(@RequestBody @Valid CreateProjectBoardRequest request) {
        ProjectBoard projectBoard = projectBoardService.create(request);

        URI uri = URI.create("/project-board/" + projectBoard.getId());

        return ResponseEntity.created(uri)
                .build();
    }

    @GetMapping("/department/{departmentType}")
    public ResponseEntity<List<ProjectBoardOverview>> getProjectBoardList(
            @PathVariable("departmentType") DepartmentType departmentType,
            Pageable pageable) {
        List<ProjectBoardOverview> projectBoardOverviews = projectBoardService.getBoardByPage(departmentType,
                pageable);

        return ResponseEntity.ok(projectBoardOverviews);
    }
}
