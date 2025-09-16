package com.dongsoop.dongsoop.search.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "boards")
public class BoardDocument {

    @Id
    private String id;

    @Field(type = FieldType.Text, analyzer = "nori")
    private String title;

    @Field(type = FieldType.Text, analyzer = "nori")
    private String content;

    @Field(name = "board_type", type = FieldType.Keyword)
    private String boardType;

    @Field(name = "board_id", type = FieldType.Long)
    private Long boardId;

    @Field(name = "author_name", type = FieldType.Keyword)
    private String authorName;

    @Field(name = "created_at", type = FieldType.Keyword)
    private String createdAt;

    @Field(name = "recruitment_start_at", type = FieldType.Date)
    private LocalDateTime recruitmentStartAt;

    @Field(name = "recruitment_end_at", type = FieldType.Date)
    private LocalDateTime recruitmentEndAt;

    @Field(name = "marketplace_status", type = FieldType.Keyword)
    private String marketplaceStatus;

    @Field(name = "marketplace_type", type = FieldType.Keyword)
    private String marketplaceType;

    @Field(name = "link", type = FieldType.Keyword)
    @JsonIgnore
    private String link;

    @Field(type = FieldType.Keyword)
    private String tags;

    @Field(type = FieldType.Integer)
    private Long price;

    @JsonProperty("noticeUrl")
    public String getNoticeUrl() {
        if (BoardType.NOTICE.getCode().equals(boardType)) {
            return link;
        }
        return null;
    }

    public String getContent() {
        if (BoardType.NOTICE.getCode().equals(boardType)) {
            return null;
        }
        return content;
    }
}