package com.oldagehome.portal.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.UUID;

@Component
public class FileUploadUtility {

    @Value("${app.upload.dir}")
    private String uploadBaseDir;

    /**
     * Saves an uploaded multipart file to a specific subdirectory inside the uploads root folder.
     * Generates a unique filename using UUID to prevent collisions.
     *
     * @param subDir Subdirectory name (e.g., "residents", "documents")
     * @param file   The MultipartFile to save
     * @return The relative filepath from the uploads root (e.g., "residents/unique-id.jpg")
     * @throws IOException If file copying fails
     */
    public String saveFile(String subDir, MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            return null;
        }

        String originalFilename = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
        String extension = getFileExtension(originalFilename);
        
        // Generate a unique filename using UUID
        String uniqueFilename = UUID.randomUUID().toString() + (extension.isEmpty() ? "" : "." + extension);

        // Resolve destination directories
        Path uploadPath = Paths.get(uploadBaseDir, subDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        Path targetFilePath = uploadPath.resolve(uniqueFilename);
        Files.copy(file.getInputStream(), targetFilePath, StandardCopyOption.REPLACE_EXISTING);

        // Return path relative to the uploads root (e.g., "residents/abc.jpg")
        return subDir + "/" + uniqueFilename;
    }

    /**
     * Deletes a file from the upload directory using its relative filepath.
     *
     * @param relativeFilePath Filepath relative to the uploads root (e.g., "residents/abc.jpg")
     * @return true if successfully deleted, false otherwise
     */
    public boolean deleteFile(String relativeFilePath) {
        if (relativeFilePath == null || relativeFilePath.isEmpty()) {
            return false;
        }
        try {
            Path filePath = Paths.get(uploadBaseDir, relativeFilePath);
            return Files.deleteIfExists(filePath);
        } catch (IOException e) {
            System.err.println("Failed to delete file: " + relativeFilePath + " -> " + e.getMessage());
            return false;
        }
    }

    private String getFileExtension(String fileName) {
        int lastIndexOf = fileName.lastIndexOf(".");
        if (lastIndexOf == -1) {
            return "";
        }
        return fileName.substring(lastIndexOf + 1);
    }
}
