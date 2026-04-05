package com.dongsoop.dongsoop.meal.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QMeal is a Querydsl query type for Meal
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QMeal extends EntityPathBase<Meal> {

    private static final long serialVersionUID = -1768621168L;

    public static final QMeal meal = new QMeal("meal");

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final StringPath dayOfWeek = createString("dayOfWeek");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final DatePath<java.time.LocalDate> mealDate = createDate("mealDate", java.time.LocalDate.class);

    public final EnumPath<MealType> mealType = createEnum("mealType", MealType.class);

    public final StringPath menuItems = createString("menuItems");

    public QMeal(String variable) {
        super(Meal.class, forVariable(variable));
    }

    public QMeal(Path<? extends Meal> path) {
        super(path.getType(), path.getMetadata());
    }

    public QMeal(PathMetadata metadata) {
        super(Meal.class, metadata);
    }

}

