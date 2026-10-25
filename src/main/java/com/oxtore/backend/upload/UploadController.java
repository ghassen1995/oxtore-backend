package com.oxtore.backend.upload;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.util.unit.DataSize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * One image per request (call once per file). Any authenticated user. Returns the public URL to render now and to
 * send back in product payloads. Reusable for logos/avatars by adding folders (see Folder).
 */
@RestController
@RequestMapping("/api/uploads")
@RequiredArgsConstructor
public class UploadController {

    /** Allow-list of target folders (?folder=...); defaults to products. */
    enum Folder {
        PRODUCTS("products"),
        BOUTIQUES("boutiques"),
        AVATARS("avatars");

        final String path;

        Folder(String path) {
            this.path = path;
        }

        static Folder fromParam(String raw) {
            if (raw == null || raw.isBlank()) return PRODUCTS;
            for (Folder f : values()) {
                if (f.path.equals(raw)) return f;
            }
            throw new IllegalArgumentException("Unknown upload folder: " + raw);
        }
    }

    private final ImageStorageService storage;

    @Value("${app.upload.max-file-size}")
    private DataSize maxFileSize;

    @PostMapping("/images")
    public ResponseEntity<Map<String, String>> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folder", required = false) String folder) throws IOException {
        Folder target = Folder.fromParam(folder);
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("No file received (multipart field \"file\")");
        }
        // Belt and braces: spring.servlet.multipart limits normally reject this before we get here.
        if (file.getSize() > maxFileSize.toBytes()) {
            throw new MaxUploadSizeExceededException(maxFileSize.toBytes());
        }

        byte[] header;
        try (InputStream in = file.getInputStream()) {
            header = in.readNBytes(ImageType.HEADER_LENGTH);
        }
        ImageType type = ImageType.detect(header)
                .orElseThrow(() -> new InvalidImageException("Only JPEG, PNG and WebP images are allowed"));

        String key;
        try (InputStream in = file.getInputStream()) {
            key = storage.store(in, type, target.path);
        }
        return ResponseEntity.status(201).body(Map.of("url", storage.publicUrl(key)));
    }
}
