package com.dongsoop.dongsoop.timetable.service;

import com.dongsoop.dongsoop.timetable.dto.CreateTimetableRequest;
import com.dongsoop.dongsoop.timetable.dto.TimetableView;
import com.dongsoop.dongsoop.timetable.dto.UpdateTimetableRequest;
import com.dongsoop.dongsoop.timetable.entity.SemesterType;
import java.time.Year;
import java.util.List;

public interface TimetableService {

    void createTimetable(CreateTimetableRequest request);

    List<TimetableView> getTimetableView(Year year, SemesterType semester);

    void deleteTimetable(Long timetableId);

    void updateTimetable(UpdateTimetableRequest request);

    void deleteTimetable(Year year, SemesterType semester);
}
