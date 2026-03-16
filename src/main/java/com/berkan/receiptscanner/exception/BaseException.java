package com.berkan.receiptscanner.exception;

/**
 * Base exception class for all custom exceptions in the application.
 * Provides common functionality and structure for exception handling.
 */
public abstract class BaseException extends RuntimeException {

    private final String errorCode;

    protected BaseException(String message) {
        super(message);
        this.errorCode = this.getClass().getSimpleName().replace("Exception", "").toUpperCase();
    }

    protected BaseException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = this.getClass().getSimpleName().replace("Exception", "").toUpperCase();
    }

    protected BaseException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    protected BaseException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
