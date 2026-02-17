package com.berkan.receiptscanner.exception;

/**
 * Exception thrown when a requested resource is not found in the database.
 */
public class ResourceNotFoundException extends BaseException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
