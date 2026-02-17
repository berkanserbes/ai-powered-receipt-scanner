package com.berkan.receiptscanner.exception;

/**
 * Exception thrown when a user attempts to access a resource they don't have permission for.
 */
public class UnauthorizedException extends BaseException {

    public UnauthorizedException(String message) {
        super(message);
    }

    public UnauthorizedException(String message, Throwable cause) {
        super(message, cause);
    }
}
