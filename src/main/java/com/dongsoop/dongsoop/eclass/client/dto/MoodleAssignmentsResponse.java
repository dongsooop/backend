package com.dongsoop.dongsoop.eclass.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MoodleAssignmentsResponse(

        List<Course> courses
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Course(long id, String fullname, List<Assignment> assignments) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Assignment(long id, long cmid, String name, long duedate, long cutoffdate) {
    }

    public List<MoodleAssignment> flatten() {
        if (courses == null) {
            return List.of();
        }

        return courses.stream()
                .filter(course -> course.assignments() != null)
                .flatMap(course -> course.assignments().stream()
                        .map(assignment -> new MoodleAssignment(assignment.id(), assignment.cmid(),
                                course.fullname(), assignment.name(), assignment.duedate(), assignment.cutoffdate())))
                .toList();
    }
}
