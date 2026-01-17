package com.dongsoop.dongsoop.s3;

import java.io.IOException;
import java.util.UUID;
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

    @Value("${cloud.aws.region.static}")
    private String region;

    @Value("${cloud.oci.namespace}")
    private String namespace;

    public String upload(MultipartFile file, String dirName, long boardId) throws IOException {
        String fileName = file.getOriginalFilename();

        int separateIndex = fileName.lastIndexOf(".");
        String extension = fileName.substring(separateIndex);
        String saveFileName = UUID.randomUUID() + extension;
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

        return String.format("https://objectstorage.%s.oraclecloud.com/n/%s/b/%s/o/%s",
                region, namespace, bucket, saveFilePath);
    }
}