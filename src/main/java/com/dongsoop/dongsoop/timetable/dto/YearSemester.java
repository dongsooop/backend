package com.dongsoop.dongsoop.timetable.dto;

import com.dongsoop.dongsoop.timetable.entity.SemesterType;
import java.time.Year;

public interface YearSemester {

    Year getYear();

    SemesterType getSemester();
}
