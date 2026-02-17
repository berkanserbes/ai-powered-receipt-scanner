package com.berkan.receiptscanner.exception;

/**
 * Exception thrown when attempting to use a revoked refresh token.
 */
public class TokenRevokedException extends BaseException {

    public TokenRevokedException(String message) {
        super(message);
    }

    public TokenRevokedException(String message, Throwable cause) {
        super(message, cause);
    }
}
