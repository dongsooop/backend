package com.dongsoop.dongsoop.s3.validator;

/**
 * 파일 앞부분의 매직바이트(시그니처)로 실제 이미지 타입을 판별한다.
 *
 * <p>클라이언트가 보낸 확장자/Content-Type을 신뢰하지 않고 바이트로만 판단하여
 * 위장 파일(예: .png로 위장한 HTML/SVG) 업로드를 차단한다.
 */
public final class ImageSignatureDetector {

    public static final int HEADER_SIZE = 16;

    private ImageSignatureDetector() {
    }

    /**
     * @param header 파일 앞 {@link #HEADER_SIZE} 바이트
     * @return 일치하는 이미지 타입, 없으면 null
     */
    public static ImageContentType detect(byte[] header) {
        if (header == null || header.length < HEADER_SIZE) {
            return null;
        }

        if (isJpeg(header)) {
            return ImageContentType.JPEG;
        }
        if (isPng(header)) {
            return ImageContentType.PNG;
        }
        if (isGif(header)) {
            return ImageContentType.GIF;
        }
        if (isWebp(header)) {
            return ImageContentType.WEBP;
        }

        return null;
    }

    // FF D8 FF
    private static boolean isJpeg(byte[] h) {
        return matches(h, 0, 0xFF, 0xD8, 0xFF);
    }

    // 89 50 4E 47 0D 0A 1A 0A
    private static boolean isPng(byte[] h) {
        return matches(h, 0, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A);
    }

    // 47 49 46 38 ("GIF8")
    private static boolean isGif(byte[] h) {
        return matches(h, 0, 0x47, 0x49, 0x46, 0x38);
    }

    // "RIFF"(0..3) + "WEBP"(8..11) + VP8 청크 헤더(12..15: "VP8 " | "VP8L" | "VP8X")
    private static boolean isWebp(byte[] h) {
        boolean riffWebp = matches(h, 0, 0x52, 0x49, 0x46, 0x46)
                && matches(h, 8, 0x57, 0x45, 0x42, 0x50);
        if (!riffWebp) {
            return false;
        }

        return matches(h, 12, 0x56, 0x50, 0x38, 0x20)   // "VP8 " (lossy)
                || matches(h, 12, 0x56, 0x50, 0x38, 0x4C) // "VP8L" (lossless)
                || matches(h, 12, 0x56, 0x50, 0x38, 0x58); // "VP8X" (extended)
    }

    private static boolean matches(byte[] header, int offset, int... expected) {
        if (header.length < offset + expected.length) {
            return false;
        }

        for (int i = 0; i < expected.length; i++) {
            if ((header[offset + i] & 0xFF) != expected[i]) {
                return false;
            }
        }

        return true;
    }
}
