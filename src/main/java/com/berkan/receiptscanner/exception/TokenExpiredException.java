package com.berkan.receiptscanner.exception;

/**
 * Exception thrown when a refresh token has expired.
 */
public class TokenExpiredException extends BaseException {

    public TokenExpiredException(String message) {
        super(message);
    }

    public TokenExpiredException(String message, Throwable cause) {
        super(message, cause);
    }
}
