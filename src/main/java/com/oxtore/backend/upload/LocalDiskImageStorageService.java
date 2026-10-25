package com.oxtore.backend.upload;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/** Stores images under {@code app.upload.dir}; served read-only at {@code /uploads/**} (see UploadWebConfig). */
@Slf4j
@Service
public class LocalDiskImageStorageService implements ImageStorageService {

    public static final String PUBLIC_PATH = "/uploads/";

    /** folder/uuid.ext — the only shape we ever generate, so anything else is rejected outright. */
    private static final Pattern KEY_PATTERN = Pattern.compile(
            "^[a-z][a-z-]*/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(jpg|png|webp)$");
    private static final Pattern FOLDER_PATTERN = Pattern.compile("^[a-z][a-z-]*$");

    private final Path root;
    private final String publicBaseUrl;

    public LocalDiskImageStorageService(@Value("${app.upload.dir}") String uploadDir,
                                        @Value("${app.public-base-url}") String publicBaseUrl) {
        this.root = Path.of(uploadDir).toAbsolutePath().normalize();
        this.publicBaseUrl = publicBaseUrl.endsWith("/")
                ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1)
                : publicBaseUrl;
    }

    /** Fail fast at startup if the directory (Docker volume) is missing or read-only. */
    @PostConstruct
    void init() throws IOException {
        Files.createDirectories(root);
        Path probe = Files.createTempFile(root, ".write-check-", ".tmp");
        Files.delete(probe);
        log.info("Image uploads stored in {} and served from {}{}", root, publicBaseUrl, PUBLIC_PATH);
    }

    public Path root() {
        return root;
    }

    @Override
    public String store(InputStream data, ImageType type, String folder) throws IOException {
        if (folder == null || !FOLDER_PATTERN.matcher(folder).matches()) {
            throw new IllegalArgumentException("Invalid upload folder");
        }
        String key = folder + "/" + UUID.randomUUID() + "." + type.extension();
        Path target = resolve(key).orElseThrow();
        Files.createDirectories(target.getParent());

        // Write to a temp file in the same directory, then move: readers never see a half-written image.
        Path tmp = Files.createTempFile(target.getParent(), ".upload-", ".tmp");
        try {
            Files.copy(data, tmp, StandardCopyOption.REPLACE_EXISTING);
            Files.move(tmp, target, StandardCopyOption.ATOMIC_MOVE);
        } finally {
            Files.deleteIfExists(tmp);
        }
        return key;
    }

    @Override
    public boolean exists(String key) {
        return resolve(key).map(Files::isRegularFile).orElse(false);
    }

    @Override
    public void delete(String key) {
        try {
            Optional<Path> path = resolve(key);
            if (path.isPresent()) {
                Files.deleteIfExists(path.get());
            }
        } catch (IOException | RuntimeException e) {
            log.warn("Could not delete uploaded image {}", key, e);
        }
    }

    @Override
    public String publicUrl(String key) {
        return publicBaseUrl + PUBLIC_PATH + key;
    }

    @Override
    public Optional<String> keyFromUrl(String urlOrPath) {
        if (urlOrPath == null || urlOrPath.isBlank()) return Optional.empty();
        String candidate = urlOrPath.trim();
        if (candidate.startsWith(publicBaseUrl + PUBLIC_PATH)) {
            candidate = candidate.substring((publicBaseUrl + PUBLIC_PATH).length());
        } else if (candidate.startsWith(PUBLIC_PATH)) {
            candidate = candidate.substring(PUBLIC_PATH.length());
        }
        return KEY_PATTERN.matcher(candidate).matches() ? Optional.of(candidate) : Optional.empty();
    }

    /** Key to path, refusing anything that is not a well-formed key or would escape the root. */
    private Optional<Path> resolve(String key) {
        if (key == null || !KEY_PATTERN.matcher(key).matches()) return Optional.empty();
        Path path = root.resolve(key).normalize();
        return path.startsWith(root) ? Optional.of(path) : Optional.empty();
    }
}
