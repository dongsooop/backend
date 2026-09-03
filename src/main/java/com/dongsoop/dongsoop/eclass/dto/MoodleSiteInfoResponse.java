package com.dongsoop.dongsoop.eclass.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MoodleSiteInfoResponse(

        long userid,
        String fullname,
        String release
) {
}
