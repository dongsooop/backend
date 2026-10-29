package com.dongsoop.dongsoop.report.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FieldResultDto {
  @JsonProperty("has_profanity")
  private boolean hasProfanity;

  public boolean hasProfanity() {
    return hasProfanity;
  }
}
