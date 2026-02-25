package com.dongsoop.dongsoop.s3;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
@Profile({"local", "test"})
@Slf4j
public class LocalS3ServiceImpl implements S3Service {

    @Override
    public String upload(MultipartFile file, String dirName, long boardId) {
        log.info("Local S3 Service - upload called. No operation performed.");
        return "uploaded_file_placeholder_url";
    }
}
