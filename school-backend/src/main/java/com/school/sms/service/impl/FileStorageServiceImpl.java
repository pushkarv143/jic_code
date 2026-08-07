package com.school.sms.service.impl;

import com.school.sms.exception.BadRequestException;
import com.school.sms.service.FileStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
public class FileStorageServiceImpl implements FileStorageService {

    private static final Set<String> PHOTO_CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final long PHOTO_MAX_BYTES = 2L * 1024 * 1024;

    private static final Set<String> DOCUMENT_CONTENT_TYPES = Set.of("application/pdf", "image/jpeg", "image/png");
    private static final long DOCUMENT_MAX_BYTES = 5L * 1024 * 1024;

    @Value("${app.upload.dir}")
    private String uploadDir;

    @Override
    public String storePhoto(MultipartFile file) {
        validate(file, PHOTO_CONTENT_TYPES, PHOTO_MAX_BYTES, "Photo");
        return store(file, "photos");
    }

    @Override
    public String storeDocument(MultipartFile file) {
        validate(file, DOCUMENT_CONTENT_TYPES, DOCUMENT_MAX_BYTES, "Document");
        return store(file, "documents");
    }

    @Override
    public void delete(String relativeUrlPath) {
        if (!StringUtils.hasText(relativeUrlPath)) {
            return;
        }
        try {
            Path target = resolvePath(relativeUrlPath);
            Files.deleteIfExists(target);
        } catch (IOException ex) {
            log.warn("Failed to delete uploaded file {}: {}", relativeUrlPath, ex.getMessage());
        }
    }

    @Override
    public byte[] readFile(String relativeUrlPath) {
        if (!StringUtils.hasText(relativeUrlPath)) {
            return null;
        }
        try {
            Path target = resolvePath(relativeUrlPath);
            if (!Files.exists(target)) {
                return null;
            }
            return Files.readAllBytes(target);
        } catch (IOException ex) {
            log.warn("Failed to read uploaded file {}: {}", relativeUrlPath, ex.getMessage());
            return null;
        }
    }

    private Path resolvePath(String relativeUrlPath) {
        String withoutPrefix = relativeUrlPath.startsWith("/uploads/")
                ? relativeUrlPath.substring("/uploads/".length())
                : relativeUrlPath;
        return Paths.get(uploadDir).resolve(withoutPrefix).normalize();
    }

    private void validate(MultipartFile file, Set<String> allowedContentTypes, long maxBytes, String label) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException(label + " file is required");
        }
        String contentType = file.getContentType();
        if (contentType == null || !allowedContentTypes.contains(contentType.toLowerCase())) {
            throw new BadRequestException(label + " must be one of: " + allowedContentTypes);
        }
        if (file.getSize() > maxBytes) {
            throw new BadRequestException(label + " must not exceed " + (maxBytes / (1024 * 1024)) + "MB");
        }
    }

    private String store(MultipartFile file, String subfolder) {
        try {
            Path targetDir = Paths.get(uploadDir, subfolder).toAbsolutePath().normalize();
            Files.createDirectories(targetDir);

            String extension = extractExtension(file.getOriginalFilename());
            String filename = UUID.randomUUID() + (extension.isEmpty() ? "" : "." + extension);
            Path targetFile = targetDir.resolve(filename);

            Files.copy(file.getInputStream(), targetFile, StandardCopyOption.REPLACE_EXISTING);

            return "/uploads/" + subfolder + "/" + filename;
        } catch (IOException ex) {
            log.error("Failed to store uploaded file: {}", ex.getMessage());
            throw new BadRequestException("Failed to store uploaded file");
        }
    }

    private String extractExtension(String originalFilename) {
        if (!StringUtils.hasText(originalFilename) || !originalFilename.contains(".")) {
            return "";
        }
        return originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase();
    }
}
