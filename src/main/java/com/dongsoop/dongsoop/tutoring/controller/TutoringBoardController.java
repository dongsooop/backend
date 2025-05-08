package com.dongsoop.dongsoop.tutoring.controller;

import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.tutoring.dto.CreateTutoringBoardRequest;
import com.dongsoop.dongsoop.tutoring.dto.TutoringBoardDetails;
import com.dongsoop.dongsoop.tutoring.dto.TutoringBoardOverview;
import com.dongsoop.dongsoop.tutoring.entity.TutoringBoard;
import com.dongsoop.dongsoop.tutoring.service.TutoringBoardService;
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
@RequestMapping("/tutoring")
@RequiredArgsConstructor
public class TutoringBoardController {

    private final TutoringBoardService tutoringBoardService;

    @GetMapping("/department/{departmentType}")
    public ResponseEntity<List<TutoringBoardOverview>> getTutoringBoardOverviews(
            @PathVariable("departmentType") DepartmentType departmentType, Pageable pageable) {
        List<TutoringBoardOverview> tutoringBoardList = tutoringBoardService.getTutoringBoardByPage(departmentType,
                pageable);
        return ResponseEntity.ok(tutoringBoardList);
    }

    @GetMapping("/{tutoringBoardId}")
    public ResponseEntity<TutoringBoardDetails> getTutoringBoard(
            @PathVariable("tutoringBoardId") Long tutoringBoardId) {
        TutoringBoardDetails tutoringBoard = tutoringBoardService.getTutoringBoardDetailsById(tutoringBoardId);
        return ResponseEntity.ok(tutoringBoard);
    }

    @PostMapping
    public ResponseEntity<Void> createTutoringBoard(@Valid @RequestBody CreateTutoringBoardRequest request) {
        TutoringBoard createdBoard = tutoringBoardService.create(request);
        URI uri = URI.create("/tutoring/" + createdBoard.getId());

        return ResponseEntity.created(uri)
                .build();
    }
}
