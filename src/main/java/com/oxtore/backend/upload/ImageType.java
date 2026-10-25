package com.oxtore.backend.upload;

import java.util.Arrays;
import java.util.Optional;

/** Accepted image formats, identified by their magic bytes (never by file name or client Content-Type). */
public enum ImageType {
    JPEG("jpg", "image/jpeg"),
    PNG("png", "image/png"),
    WEBP("webp", "image/webp");

    /** Enough bytes to recognise every type above (WebP needs 12). */
    public static final int HEADER_LENGTH = 12;

    private static final byte[] JPEG_MAGIC = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] PNG_MAGIC = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};
    private static final byte[] RIFF = {'R', 'I', 'F', 'F'};
    private static final byte[] WEBP_TAG = {'W', 'E', 'B', 'P'};

    private final String extension;
    private final String contentType;

    ImageType(String extension, String contentType) {
        this.extension = extension;
        this.contentType = contentType;
    }

    public String extension() {
        return extension;
    }

    public String contentType() {
        return contentType;
    }

    public static Optional<ImageType> detect(byte[] header) {
        if (header == null) return Optional.empty();
        if (startsWith(header, 0, JPEG_MAGIC)) return Optional.of(JPEG);
        if (startsWith(header, 0, PNG_MAGIC)) return Optional.of(PNG);
        if (startsWith(header, 0, RIFF) && startsWith(header, 8, WEBP_TAG)) return Optional.of(WEBP);
        return Optional.empty();
    }

    private static boolean startsWith(byte[] data, int offset, byte[] prefix) {
        return data.length >= offset + prefix.length
                && Arrays.equals(data, offset, offset + prefix.length, prefix, 0, prefix.length);
    }
}
