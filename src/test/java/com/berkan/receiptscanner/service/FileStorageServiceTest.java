package com.berkan.receiptscanner.service;

import com.berkan.receiptscanner.exception.FileStorageException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("FileStorageService Tests")
class FileStorageServiceTest {

    @TempDir
    Path tempDir;

    private FileStorageService createService() {
        return new FileStorageService(tempDir.toString());
    }

    // ========== STORE FILE ==========

    @Nested
    @DisplayName("storeFile()")
    class StoreFile {

        @Test
        @DisplayName("Should store JPEG file successfully")
        void shouldStoreJpegFile() {
            FileStorageService service = createService();
            MockMultipartFile file = new MockMultipartFile(
                    "file", "receipt.jpg", "image/jpeg", new byte[]{1, 2, 3});

            String storedFileName = service.storeFile(file);

                assertAll("File storage assertions",
                    () -> assertNotNull(storedFileName),
                    () -> assertTrue(storedFileName.endsWith(".jpg")),
                    () -> assertTrue(java.nio.file.Files.exists(tempDir.resolve(storedFileName)))
            );
        }

        @Test
        @DisplayName("Should store PNG file successfully")
        void shouldStorePngFile() {
            FileStorageService service = createService();
            MockMultipartFile file = new MockMultipartFile(
                    "file", "receipt.png", "image/png", new byte[]{1, 2, 3});

            String storedFileName = service.storeFile(file);

            assertNotNull(storedFileName);
            assertTrue(storedFileName.endsWith(".png"));
        }

        @Test
        @DisplayName("Should store PDF file successfully")
        void shouldStorePdfFile() {
            FileStorageService service = createService();
            MockMultipartFile file = new MockMultipartFile(
                    "file", "receipt.pdf", "application/pdf", new byte[]{1, 2, 3});

            String storedFileName = service.storeFile(file);

            assertNotNull(storedFileName);
            assertTrue(storedFileName.endsWith(".pdf"));
        }

        @Test
        @DisplayName("Should reject invalid file type")
        void shouldRejectInvalidFileType() {
            FileStorageService service = createService();
            MockMultipartFile file = new MockMultipartFile(
                    "file", "document.txt", "text/plain", new byte[]{1, 2, 3});

            assertThrows(FileStorageException.class, () -> service.storeFile(file));
        }

        @Test
        @DisplayName("Should reject blank file name")
        void shouldRejectBlankFileName() {
            FileStorageService service = createService();
            MockMultipartFile file = new MockMultipartFile(
                    "file", "", "image/jpeg", new byte[]{1, 2, 3});

            assertThrows(FileStorageException.class, () -> service.storeFile(file));
        }

        @Test
        @DisplayName("Should reject null file name")
        void shouldRejectNullFileName() {
            FileStorageService service = createService();
            MockMultipartFile file = new MockMultipartFile(
                    "file", null, "image/jpeg", new byte[]{1, 2, 3});

            assertThrows(FileStorageException.class, () -> service.storeFile(file));
        }
    }

    // ========== LOAD FILE ==========

    @Nested
    @DisplayName("loadFile()")
    class LoadFile {

        @Test
        @DisplayName("Should load stored file successfully")
        void shouldLoadStoredFile() {
            FileStorageService service = createService();
            byte[] content = new byte[]{10, 20, 30, 40};
            MockMultipartFile file = new MockMultipartFile(
                    "file", "receipt.jpg", "image/jpeg", content);

            String storedFileName = service.storeFile(file);
            var storedFileData = service.loadFile(storedFileName);

                assertAll("File loading assertions",
                    () -> assertNotNull(storedFileData),
                    () -> assertNotNull(storedFileData.content()),
                    () -> assertEquals(4, storedFileData.content().length)
            );
        }

        @Test
        @DisplayName("Should reject blank file reference")
        void shouldRejectBlankFileReference() {
            FileStorageService service = createService();

            assertThrows(FileStorageException.class, () -> service.loadFile(""));
        }

        @Test
        @DisplayName("Should reject null file reference")
        void shouldRejectNullFileReference() {
            FileStorageService service = createService();

            assertThrows(FileStorageException.class, () -> service.loadFile(null));
        }
    }
}
