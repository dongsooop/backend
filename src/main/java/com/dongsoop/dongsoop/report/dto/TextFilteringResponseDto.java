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

    public boolean hasProfanity() {
        return hasTitleProfanity() || hasTagsProfanity() || hasContentProfanity();
    }

    private boolean hasTitleProfanity() {
        return title != null && title.hasProfanity();
    }

    private boolean hasTagsProfanity() {
        return tags != null && tags.hasProfanity();
    }

    private boolean hasContentProfanity() {
        return content != null && content.hasProfanity();
    }
}