package com.berkan.receiptscanner.exception;

/**
 * Exception thrown when attempting to create a resource that already exists.
 */
public class DuplicateResourceException extends BaseException {

    public DuplicateResourceException(String message) {
        super(message);
    }

    public DuplicateResourceException(String message, Throwable cause) {
        super(message, cause);
    }
}
