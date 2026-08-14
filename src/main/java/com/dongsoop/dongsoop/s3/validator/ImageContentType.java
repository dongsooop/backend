package com.dongsoop.dongsoop.s3.validator;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 업로드를 허용하는 이미지 타입. 저장 시 사용할 정규 MIME과 확장자를 보유한다.
 *
 * <p>실제 바이트 시그니처 판별은 {@link ImageSignatureDetector}가 담당한다.
 */
@Getter
@RequiredArgsConstructor
public enum ImageContentType {

    JPEG("image/jpeg", "jpg"),
    PNG("image/png", "png"),
    GIF("image/gif", "gif"),
    WEBP("image/webp", "webp");

    private final String mimeType;
    private final String extension;
}
