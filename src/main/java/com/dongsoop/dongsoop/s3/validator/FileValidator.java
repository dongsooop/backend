package com.dongsoop.dongsoop.s3.validator;

import com.dongsoop.dongsoop.s3.exception.FileTooLargeException;
import com.dongsoop.dongsoop.s3.exception.InvalidFileException;
import java.io.IOException;
import java.io.InputStream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * 업로드 파일을 검증한다: 빈 파일 차단, 크기 상한, 매직바이트 기반 실제 이미지 타입 판별.
 *
 * <p>확장자/Content-Type 같은 클라이언트 제공 값은 신뢰하지 않으며,
 * 반환된 {@link ImageContentType}으로 저장 시 사용할 정규 MIME/확장자를 결정한다.
 */
@Component
@Slf4j
public class FileValidator {

    private static final long MAX_SIZE_BYTES = 10L * 1024 * 1024; // 10MB

    /**
     * @return 매직바이트로 판별된 이미지 타입 (저장 시 정규 MIME/확장자로 사용)
     * @throws InvalidFileException  파일이 비었거나 허용되지 않은 형식일 때
     * @throws FileTooLargeException 크기 상한을 초과했을 때
     */
    public ImageContentType validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileException("업로드할 파일이 없습니다.");
        }

        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new FileTooLargeException(MAX_SIZE_BYTES);
        }

        ImageContentType detectedType = detectType(file);
        if (detectedType == null) {
            throw new InvalidFileException("허용되지 않은 파일 형식입니다. (jpg, png, gif, webp 이미지만 가능)");
        }

        return detectedType;
    }

    private ImageContentType detectType(MultipartFile file) {
        byte[] header = new byte[ImageSignatureDetector.HEADER_SIZE];

        try (InputStream inputStream = file.getInputStream()) {
            int read = inputStream.readNBytes(header, 0, header.length);
            if (read < ImageSignatureDetector.HEADER_SIZE) {
                return null;
            }
        } catch (IOException e) {
            log.warn("Failed to read uploaded file header", e);
            throw new InvalidFileException("파일을 읽을 수 없습니다.");
        }

        return ImageSignatureDetector.detect(header);
    }
}
