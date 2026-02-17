package com.berkan.receiptscanner.exception;

/**
 * Exception thrown when file storage operations fail.
 * This includes file upload, retrieval, or deletion errors.
 */
public class FileStorageException extends BaseException {

    public FileStorageException(String message) {
        super(message);
    }

    public FileStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
