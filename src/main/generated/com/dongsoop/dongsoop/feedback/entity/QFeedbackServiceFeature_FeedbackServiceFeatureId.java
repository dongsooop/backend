package com.dongsoop.dongsoop.feedback.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QFeedbackServiceFeature_FeedbackServiceFeatureId is a Querydsl query type for FeedbackServiceFeatureId
 */
@Generated("com.querydsl.codegen.DefaultEmbeddableSerializer")
public class QFeedbackServiceFeature_FeedbackServiceFeatureId extends BeanPath<FeedbackServiceFeature.FeedbackServiceFeatureId> {

    private static final long serialVersionUID = -1088905382L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QFeedbackServiceFeature_FeedbackServiceFeatureId feedbackServiceFeatureId = new QFeedbackServiceFeature_FeedbackServiceFeatureId("feedbackServiceFeatureId");

    public final QFeedback feedback;

    public final EnumPath<ServiceFeature> serviceFeature = createEnum("serviceFeature", ServiceFeature.class);

    public QFeedbackServiceFeature_FeedbackServiceFeatureId(String variable) {
        this(FeedbackServiceFeature.FeedbackServiceFeatureId.class, forVariable(variable), INITS);
    }

    public QFeedbackServiceFeature_FeedbackServiceFeatureId(Path<? extends FeedbackServiceFeature.FeedbackServiceFeatureId> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QFeedbackServiceFeature_FeedbackServiceFeatureId(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QFeedbackServiceFeature_FeedbackServiceFeatureId(PathMetadata metadata, PathInits inits) {
        this(FeedbackServiceFeature.FeedbackServiceFeatureId.class, metadata, inits);
    }

    public QFeedbackServiceFeature_FeedbackServiceFeatureId(Class<? extends FeedbackServiceFeature.FeedbackServiceFeatureId> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.feedback = inits.isInitialized("feedback") ? new QFeedback(forProperty("feedback"), inits.get("feedback")) : null;
    }

}

