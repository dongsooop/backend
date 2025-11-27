package com.dongsoop.dongsoop.search.entity;

import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "boards")
public class RestaurantDocument {

    @Id
    private String id;

    @Field(name = "board_id", type = FieldType.Long)
    private Long restaurantId;

    @Field(name = "title", type = FieldType.Text, analyzer = "nori")
    private String name;

    @Field(name = "content", type = FieldType.Keyword)
    private String category;

    @Field(type = FieldType.Text, analyzer = "nori")
    private String tags;

    @Field(name = "link", type = FieldType.Keyword)
    private String placeUrl;

    @Field(name = "contact_count", type = FieldType.Integer)
    private Integer likeCount;

    @Field(type = FieldType.Double)
    private Double distance;

    @Field(name = "external_map_id", type = FieldType.Keyword)
    private String externalMapId;
}