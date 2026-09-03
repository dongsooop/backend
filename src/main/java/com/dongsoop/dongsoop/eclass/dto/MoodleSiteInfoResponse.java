package com.dongsoop.dongsoop.eclass.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MoodleSiteInfoResponse(

        long userid,
        String fullname,
        String release
) {
}
