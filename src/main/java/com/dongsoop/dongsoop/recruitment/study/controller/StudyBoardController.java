package com.dongsoop.dongsoop.recruitment.study.controller;

import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.recruitment.study.dto.CreateStudyBoardRequest;
import com.dongsoop.dongsoop.recruitment.study.dto.StudyBoardDetails;
import com.dongsoop.dongsoop.recruitment.study.dto.StudyBoardOverview;
import com.dongsoop.dongsoop.recruitment.study.entity.StudyBoard;
import com.dongsoop.dongsoop.recruitment.study.service.StudyBoardService;
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
@RequestMapping("/study-board")
@RequiredArgsConstructor
public class StudyBoardController {

    private final StudyBoardService studyBoardService;

    @GetMapping("/{studyBoardId}")
    public ResponseEntity<StudyBoardDetails> getStudyBoardDetails(@PathVariable Long studyBoardId) {
        StudyBoardDetails studyBoardDetails = studyBoardService.getBoardDetailsById(studyBoardId);

        return ResponseEntity.ok(studyBoardDetails);
    }

    @PostMapping
    public ResponseEntity<Void> createStudyBoard(@RequestBody @Valid CreateStudyBoardRequest request) {
        StudyBoard studyBoard = studyBoardService.create(request);

        URI uri = URI.create("/study-board/" + studyBoard.getId());

        return ResponseEntity.created(uri)
                .build();
    }

    @GetMapping("/department/{departmentType}")
    public ResponseEntity<List<StudyBoardOverview>> getStudyBoardListByDepartmentType(
            @PathVariable("departmentType") DepartmentType departmentType,
            Pageable pageable) {
        List<StudyBoardOverview> studyBoardOverviews = studyBoardService.getBoardByPageAndDepartmentType(departmentType,
                pageable);

        return ResponseEntity.ok(studyBoardOverviews);
    }

    @GetMapping
    public ResponseEntity<List<StudyBoardOverview>> getStudyBoardList(Pageable pageable) {
        List<StudyBoardOverview> studyBoardOverviews = studyBoardService.getBoardByPage(pageable);

        return ResponseEntity.ok(studyBoardOverviews);
    }
}
