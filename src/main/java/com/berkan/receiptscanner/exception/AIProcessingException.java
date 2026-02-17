package com.berkan.receiptscanner.exception;

/**
 * Exception thrown when AI processing fails.
 */
public class AIProcessingException extends BaseException {

    public AIProcessingException(String message) {
        super(message);
    }

    public AIProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
