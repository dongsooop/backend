package com.dongsoop.dongsoop.s3;

import java.io.IOException;
import org.springframework.web.multipart.MultipartFile;

public interface S3Service {

    String upload(MultipartFile file, String dirName, long boardId) throws IOException;
}
