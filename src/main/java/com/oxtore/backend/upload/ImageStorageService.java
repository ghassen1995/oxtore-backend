package com.oxtore.backend.upload;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

/**
 * Where uploaded images live. Callers only ever handle storage keys ("products/{uuid}.webp") and public URLs,
 * so a local-disk implementation can be swapped for S3 / R2 / MinIO without touching them.
 */
public interface ImageStorageService {

    /** Stores the bytes under a new random name in {@code folder}; returns the storage key. */
    String store(InputStream data, ImageType type, String folder) throws IOException;

    boolean exists(String key);

    /** Best effort: failures are logged, never thrown. */
    void delete(String key);

    /** Absolute URL a browser can load without authentication. */
    String publicUrl(String key);

    /**
     * Parses one of our own URLs (absolute or "/uploads/..." relative) or a bare key back into a storage key.
     * Empty for anything else: foreign URLs, data URLs, malformed or traversal attempts.
     */
    Optional<String> keyFromUrl(String urlOrPath);
}
