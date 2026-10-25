package com.oxtore.backend.upload;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Validates a single image field (e.g. a boutique logo) holding a URL returned by POST /api/uploads/images. */
@Component
@RequiredArgsConstructor
public class ImageReferenceInput {

    private final ImageStorageService storage;

    /**
     * Storage key for {@code raw}, or null when absent/blank. Rejects anything that is not one of our uploads
     * in {@code folder} (foreign URLs, base64 data URLs, unknown files).
     */
    public String optionalKey(Object raw, String folder, String fieldName) {
        if (raw == null || (raw instanceof String s && s.isBlank())) return null;
        String key = raw instanceof String s ? storage.keyFromUrl(s).orElse(null) : null;
        if (key == null || !key.startsWith(folder + "/") || !storage.exists(key)) {
            throw new InvalidImageReferenceException(fieldName + " was not uploaded through /api/uploads/images");
        }
        return key;
    }
}
