package com.dongsoop.dongsoop.feedback.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QFeedbackServiceFeature is a Querydsl query type for FeedbackServiceFeature
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QFeedbackServiceFeature extends EntityPathBase<FeedbackServiceFeature> {

    private static final long serialVersionUID = 1510177205L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QFeedbackServiceFeature feedbackServiceFeature = new QFeedbackServiceFeature("feedbackServiceFeature");

    public final QFeedbackServiceFeature_FeedbackServiceFeatureId id;

    public QFeedbackServiceFeature(String variable) {
        this(FeedbackServiceFeature.class, forVariable(variable), INITS);
    }

    public QFeedbackServiceFeature(Path<? extends FeedbackServiceFeature> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QFeedbackServiceFeature(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QFeedbackServiceFeature(PathMetadata metadata, PathInits inits) {
        this(FeedbackServiceFeature.class, metadata, inits);
    }

    public QFeedbackServiceFeature(Class<? extends FeedbackServiceFeature> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.id = inits.isInitialized("id") ? new QFeedbackServiceFeature_FeedbackServiceFeatureId(forProperty("id"), inits.get("id")) : null;
    }

}

