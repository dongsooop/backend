package com.dongsoop.dongsoop.s3;

import com.dongsoop.dongsoop.s3.validator.FileValidator;
import com.dongsoop.dongsoop.s3.validator.ImageContentType;
import java.io.IOException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
@Profile("prod")
@RequiredArgsConstructor
public class S3ServiceImpl implements S3Service {

    private final S3Client client;
    private final FileValidator fileValidator;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${cloud.aws.region.static}")
    private String region;

    @Value("${cloud.oci.namespace}")
    private String namespace;

    public String upload(MultipartFile file, String dirName, long boardId) throws IOException {
        // 매직바이트로 실제 이미지 타입을 검증하고, 저장명/Content-Type은 클라이언트 입력이 아닌 판별 결과로 결정한다.
        ImageContentType contentType = fileValidator.validate(file);

        String saveFileName = UUID.randomUUID() + "." + contentType.getExtension();
        String saveFilePath = dirName + "/" + boardId + "/" + saveFileName;

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(saveFilePath)
                .contentType(contentType.getMimeType())
                .build();

        client.putObject(
                putObjectRequest,
                RequestBody.fromInputStream(file.getInputStream(), file.getSize())
        );

        return String.format("https://objectstorage.%s.oraclecloud.com/n/%s/b/%s/o/%s",
                region, namespace, bucket, saveFilePath);
    }
}