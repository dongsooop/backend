package com.dongsoop.dongsoop.report.dto;

public class TextFilteringResponseDto {
    private FieldResultDto title;
    private FieldResultDto tags;
    private FieldResultDto content;

    public FieldResultDto getTitle() {
        return title;
    }

    public FieldResultDto getTags() {
        return tags;
    }

    public FieldResultDto getContent() {
        return content;
    }
}