package com.dongsoop.dongsoop.s3;

import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
@RequiredArgsConstructor
public class S3ServiceImpl implements S3Service {

    private final S3Client client;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    public String upload(MultipartFile file, String dirName, long boardId) throws IOException {
        String fileName = file.getOriginalFilename();

        int separateIndex = fileName.lastIndexOf(".");
        String saveFileName = fileName.substring(separateIndex);
        String saveFilePath = dirName + "/" + boardId + "/" + saveFileName;

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(saveFilePath)
                .contentType(file.getContentType())
                .build();

        client.putObject(
                putObjectRequest,
                RequestBody.fromInputStream(file.getInputStream(), file.getSize())
        );

        return "https://" + saveFilePath + ".s3.amazonaws.com/" + fileName;
    }
}
