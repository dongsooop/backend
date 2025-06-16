package com.dongsoop.dongsoop.timetable.controller;

import com.dongsoop.dongsoop.role.entity.RoleType;
import com.dongsoop.dongsoop.timetable.dto.CreateTimetableRequest;
import com.dongsoop.dongsoop.timetable.dto.TimetableView;
import com.dongsoop.dongsoop.timetable.entity.SemesterType;
import com.dongsoop.dongsoop.timetable.service.TimetableService;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.Year;
import java.util.List;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
@RequestMapping("/timetable")
public class TimetableController {

    private final TimetableService timetableService;

    @GetMapping("/{year}/{semester}")
    public ResponseEntity<List<TimetableView>> getTimetable(@PathVariable("year") Year year,
                                                            @PathVariable("semester") SemesterType semester) {
        List<TimetableView> timetable = timetableService.getTimetableView(year, semester);

        return ResponseEntity.ok(timetable);
    }

    @PostMapping
    @Secured(RoleType.USER_ROLE)
    public ResponseEntity<Void> createTimetable(@RequestBody @Valid CreateTimetableRequest request) {
        timetableService.createTimetable(request);
        URI uri = URI.create("/timetable/" + request.year() + "/" + request.semester());

        return ResponseEntity.created(uri)
                .build();
    }

    @DeleteMapping("/{timetableId}")
    @Secured(RoleType.USER_ROLE)
    public ResponseEntity<Void> deleteTimetable(@PathVariable("timetableId") Long timetableId) {
        timetableService.deleteTimetable(timetableId);
        return ResponseEntity.noContent().build();
    }
}
