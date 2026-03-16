package com.berkan.receiptscanner.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.berkan.receiptscanner.dto.response.StoredFileData;
import com.berkan.receiptscanner.exception.FileStorageException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
public class FileStorageService {

    private final Path uploadDir;
    private final Cloudinary cloudinary;
    private final String cloudinaryFolder;
    private final RestClient restClient;
    private final boolean cloudStorageEnabled;

    public FileStorageService(@Value("${file.upload-dir}") String uploadDir) {
        this(uploadDir, "", "", "", "receipts");
    }

    @Autowired
    public FileStorageService(
            @Value("${file.upload-dir}") String uploadDir,
            @Value("${cloudinary.cloud-name:}") String cloudName,
            @Value("${cloudinary.api-key:}") String apiKey,
            @Value("${cloudinary.api-secret:}") String apiSecret,
            @Value("${cloudinary.folder:receipts}") String cloudinaryFolder) {
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.cloudinaryFolder = cloudinaryFolder;
        this.restClient = RestClient.builder().build();

        if (!cloudName.isBlank() && !apiKey.isBlank() && !apiSecret.isBlank()) {
            this.cloudinary = new Cloudinary(ObjectUtils.asMap(
                    "cloud_name", cloudName,
                    "api_key", apiKey,
                    "api_secret", apiSecret,
                    "secure", true
            ));
            this.cloudStorageEnabled = true;
        } else {
            this.cloudinary = null;
            this.cloudStorageEnabled = false;
        }

        try {
            Files.createDirectories(this.uploadDir);
        } catch (IOException ex) {
            throw new FileStorageException("Could not create upload directory", ex);
        }
    }

    public String storeFile(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new FileStorageException("Invalid file name");
        }

        // Validate file type
        String contentType = file.getContentType();
        if (contentType == null || (!contentType.startsWith("image/") && !contentType.equals("application/pdf"))) {
            throw new FileStorageException("Only image files (JPEG, PNG) and PDF files are allowed");
        }

        // Generate unique file name
        String extension = extractExtension(originalFilename);
        String storedFileName = UUID.randomUUID() + extension;

        if (cloudStorageEnabled) {
            return storeFileToCloudinary(file, storedFileName);
        }

        return storeFileLocally(file, storedFileName, originalFilename);
    }

    public StoredFileData loadFile(String fileReference) {
        if (fileReference == null || fileReference.isBlank()) {
            throw new FileStorageException("Invalid file reference");
        }

        if (fileReference.startsWith("http://") || fileReference.startsWith("https://")) {
            return loadRemoteFile(fileReference);
        }

        return loadLocalFile(fileReference);
    }

    private String storeFileToCloudinary(MultipartFile file, String publicId) {
        try {
            Map<?, ?> uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                    "resource_type", "auto",
                    "public_id", publicId,
                    "folder", cloudinaryFolder,
                    "overwrite", true
            ));
            Object secureUrl = uploadResult.get("secure_url");
            if (secureUrl == null) {
                throw new FileStorageException("Cloud upload did not return URL");
            }
            return secureUrl.toString();
        } catch (IOException | RuntimeException ex) {
            throw new FileStorageException("Could not upload file to cloud storage", ex);
        }
    }

    private String storeFileLocally(MultipartFile file, String storedFileName, String originalFilename) {
        try {
            Path targetPath = this.uploadDir.resolve(storedFileName).normalize();
            if (!targetPath.startsWith(this.uploadDir)) {
                throw new FileStorageException("Invalid storage path");
            }
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            return storedFileName;
        } catch (IOException ex) {
            throw new FileStorageException("Could not store file: " + originalFilename, ex);
        }
    }

    private StoredFileData loadRemoteFile(String fileUrl) {
        try {
            ResponseEntity<byte[]> response = restClient.get()
                    .uri(fileUrl)
                    .retrieve()
                    .toEntity(byte[].class);

            byte[] content = Objects.requireNonNullElse(response.getBody(), new byte[0]);
            String contentType = response.getHeaders().getFirst("Content-Type");
            if (contentType == null || contentType.isBlank()) {
                contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
            }

            String fileName = extractFileNameFromUrl(fileUrl);
            return new StoredFileData(fileName, content, contentType);
        } catch (RuntimeException ex) {
            throw new FileStorageException("Could not download file from cloud storage", ex);
        }
    }

    private StoredFileData loadLocalFile(String storedFileName) {
        try {
            Path targetPath = this.uploadDir.resolve(storedFileName).normalize();
            if (!targetPath.startsWith(this.uploadDir)) {
                throw new FileStorageException("Invalid file path");
            }
            byte[] content = Files.readAllBytes(targetPath);
            String contentType = Files.probeContentType(targetPath);
            if (contentType == null || contentType.isBlank()) {
                contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
            }
            return new StoredFileData(storedFileName, content, contentType);
        } catch (IOException ex) {
            throw new FileStorageException("Could not read file: " + storedFileName, ex);
        }
    }

    private String extractExtension(String originalFilename) {
        int dotIndex = originalFilename.lastIndexOf('.');
        return dotIndex >= 0 ? originalFilename.substring(dotIndex) : "";
    }

    private String extractFileNameFromUrl(String fileUrl) {
        int queryIndex = fileUrl.indexOf('?');
        String cleanUrl = queryIndex >= 0 ? fileUrl.substring(0, queryIndex) : fileUrl;
        int slashIndex = cleanUrl.lastIndexOf('/');
        return slashIndex >= 0 ? cleanUrl.substring(slashIndex + 1) : "receipt-file";
    }
}
