package com.dongsoop.dongsoop.tutoring.controller;

import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.tutoring.dto.CreateTutoringBoardRequest;
import com.dongsoop.dongsoop.tutoring.dto.TutoringBoardOverview;
import com.dongsoop.dongsoop.tutoring.service.TutoringBoardService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
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

    @GetMapping("/{departmentType}")
    public ResponseEntity<List<TutoringBoardOverview>> getTutoringBoardOverviews(
            @PathVariable DepartmentType departmentType) {
        List<TutoringBoardOverview> tutoringBoardList = tutoringBoardService.getAllTutoringBoard(departmentType);
        return ResponseEntity.ok(tutoringBoardList);
    }

    @PostMapping
    public ResponseEntity<Void> createTutoringBoard(@Valid @RequestBody CreateTutoringBoardRequest request) {
        tutoringBoardService.create(request);

        return ResponseEntity.ok()
                .build();
    }
}
