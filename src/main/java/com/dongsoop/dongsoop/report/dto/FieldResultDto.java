package com.dongsoop.dongsoop.report.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FieldResultDto {
    @JsonProperty("has_profanity")
    private boolean has_profanity;

    public boolean hasProfanity() {
        return has_profanity;
    }
}