package com.dongsoop.dongsoop.s3;

import com.dongsoop.dongsoop.s3.validator.FileValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
@Profile({"local", "test"})
@RequiredArgsConstructor
@Slf4j
public class LocalS3ServiceImpl implements S3Service {

    private final FileValidator fileValidator;

    @Override
    public String upload(MultipartFile file, String dirName, long boardId) {
        // 실제 업로드는 하지 않지만, 검증 동작은 운영과 동일하게 유지하여 개발 단계에서 결함을 잡는다.
        fileValidator.validate(file);

        log.info("Local S3 Service - upload called. No operation performed.");
        return "uploaded_file_placeholder_url";
    }
}
