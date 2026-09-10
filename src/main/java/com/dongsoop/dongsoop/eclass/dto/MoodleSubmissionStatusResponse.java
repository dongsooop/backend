package com.dongsoop.dongsoop.eclass.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MoodleSubmissionStatusResponse(

        LastAttempt lastattempt
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record LastAttempt(Submission submission) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Submission(String status) {
    }

    public String submissionStatus() {
        if (lastattempt == null || lastattempt.submission() == null) {
            return null;
        }

        return lastattempt.submission().status();
    }
}
