package com.bank.exception;

/**
 * Base unchecked exception for all banking domain errors.
 */
public class BankException extends RuntimeException {
    public BankException(String message) {
        super(message);
    }

    public BankException(String message, Throwable cause) {
        super(message, cause);
    }
}
