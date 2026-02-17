package com.berkan.receiptscanner.exception;

/**
 * Exception thrown when request validation fails or invalid data is provided.
 */
public class InvalidRequestException extends BaseException {

    public InvalidRequestException(String message) {
        super(message);
    }

    public InvalidRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}
