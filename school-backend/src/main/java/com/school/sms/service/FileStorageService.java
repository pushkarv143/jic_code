package com.school.sms.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {

    /**
     * Validates and stores a student/teacher profile photo under the
     * {@code photos/} subfolder. Allowed types: jpg/png/webp, max 2MB.
     *
     * @return the relative URL path (e.g. {@code /uploads/photos/<uuid>.jpg}) to persist on the entity.
     */
    String storePhoto(MultipartFile file);

    /**
     * Validates and stores a student document under the {@code documents/}
     * subfolder. Allowed types: pdf/jpg/png, max 5MB.
     *
     * @return the relative URL path (e.g. {@code /uploads/documents/<uuid>.pdf}) to persist on the entity.
     */
    String storeDocument(MultipartFile file);

    /**
     * Validates and stores a study material under the {@code materials/} subfolder.
     * Accepts the document/slide/spreadsheet formats teachers share, plus images and
     * zip archives, up to 25MB — a wider list and a higher ceiling than
     * {@link #storeDocument}, which is for scanned student paperwork.
     *
     * @return the relative URL path (e.g. {@code /uploads/materials/<uuid>.pdf}) to persist on the entity.
     */
    String storeMaterial(MultipartFile file);

    /**
     * Deletes a previously stored file given its relative URL path (as returned
     * by {@link #storePhoto} / {@link #storeDocument}). Silently no-ops if the
     * file cannot be found.
     */
    void delete(String relativeUrlPath);

    /**
     * Reads back the raw bytes of a previously stored file given its relative
     * URL path (as returned by {@link #storePhoto} / {@link #storeDocument}).
     * Used by the ID-card PDF generator to embed a student/teacher's photo.
     *
     * @return the file's bytes, or {@code null} if the path is blank or the file cannot be found.
     */
    byte[] readFile(String relativeUrlPath);
}
