package com.dongsoop.dongsoop.s3;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.dongsoop.dongsoop.s3.exception.InvalidFileNameException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class S3ServiceImplTest {

    private final S3ServiceImpl s3Service = new S3ServiceImpl(null);

    @Test
    @DisplayName("확장자가 없는 파일은 업로드를 거부한다")
    void rejectsFileWithoutExtension() {
        MockMultipartFile file = new MockMultipartFile("file", "profile", "image/png", new byte[]{1});

        assertThrows(InvalidFileNameException.class, () -> s3Service.upload(file, "member", 1L));
    }

    @Test
    @DisplayName("이름이 비어 있는 파일은 업로드를 거부한다")
    void rejectsFileWithoutName() {
        MockMultipartFile file = new MockMultipartFile("file", null, "image/png", new byte[]{1});

        assertThrows(InvalidFileNameException.class, () -> s3Service.upload(file, "member", 1L));
    }
}
