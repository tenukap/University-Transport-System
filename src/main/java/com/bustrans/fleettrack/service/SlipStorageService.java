package com.bustrans.fleettrack.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class SlipStorageService {

    private static final long MAX_BYTES = 5L * 1024 * 1024; // 5 MB

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "pdf");
    private static final Set<String> ALLOWED_CONTENT_TYPES =
            Set.of("image/jpeg", "image/png", "application/pdf");

    // Derive content type from stored extension when serving the file
    private static final Map<String, String> EXT_TO_CONTENT_TYPE = Map.of(
            "jpg",  "image/jpeg",
            "jpeg", "image/jpeg",
            "png",  "image/png",
            "pdf",  "application/pdf"
    );

    @Value("${app.slip-dir:./uploads/slips}")
    private String slipDir;

    /**
     * Validates size, content type, and file extension.
     * Throws 400 with a readable message on any failure.
     */
    public void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A payment slip is required");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "File size must not exceed 5 MB");
        }
        // Check declared content type
        String ct = Optional.ofNullable(file.getContentType()).map(String::toLowerCase).orElse("");
        if (!ALLOWED_CONTENT_TYPES.contains(ct)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "File type not allowed. Accepted: JPG, PNG, PDF");
        }
        // Also check file extension — guards against mismatched Content-Type headers
        String ext = extension(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "File type not allowed. Accepted: JPG, PNG, PDF");
        }
    }

    /**
     * Stores the file under a UUID-based name and returns that name.
     * The original filename is NEVER used in any path (path-traversal prevention).
     */
    public String store(MultipartFile file) throws IOException {
        String ext = resolveExtension(file);
        String storedName = UUID.randomUUID() + "." + ext;
        Path dir = Paths.get(slipDir);
        Files.createDirectories(dir); // Create upload folder if it does not exist yet
        Files.copy(file.getInputStream(), dir.resolve(storedName), StandardCopyOption.REPLACE_EXISTING);
        return storedName;
    }

    /**
     * Resolves the absolute Path for a stored filename.
     * Guards against path-traversal: the resolved path must remain inside slipDir.
     */
    public Path load(String storedName) {
        if (storedName == null || storedName.isBlank()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Slip not found");
        }
        Path base = Paths.get(slipDir).toAbsolutePath().normalize();
        Path resolved = base.resolve(storedName).normalize();
        // Reject any path that escapes the upload directory
        if (!resolved.startsWith(base)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Slip not found");
        }
        return resolved;
    }

    /** Maps a stored filename extension to the HTTP Content-Type to serve. */
    public String contentType(String storedName) {
        String ext = extension(storedName);
        return EXT_TO_CONTENT_TYPE.getOrDefault(ext, "application/octet-stream");
    }

    // ── Private helpers ────────────────────────────────────────────────────

    private String extension(String filename) {
        if (filename == null) return "";
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot + 1).toLowerCase() : "";
    }

    private String resolveExtension(MultipartFile file) {
        String ext = extension(file.getOriginalFilename());
        return "jpeg".equals(ext) ? "jpg" : ext;
    }
}
