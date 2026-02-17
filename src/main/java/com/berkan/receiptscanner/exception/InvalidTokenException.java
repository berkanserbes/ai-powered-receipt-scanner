package com.berkan.receiptscanner.exception;

// Exception for JWT token validation failures
public class InvalidTokenException extends BaseException {

    public InvalidTokenException(String message) {
        super(message);
    }

    public InvalidTokenException(String message, Throwable cause) {
        super(message, cause);
    }
}
