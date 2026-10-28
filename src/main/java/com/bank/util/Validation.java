package com.bank.util;

import com.bank.domain.AccountType;
import com.bank.exception.ValidationException;

import java.util.regex.Pattern;

/**
 * Validation utilities for bank system inputs.
 */
public final class Validation {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private Validation() {}

    public static void validateName(String name) {
        if (name == null || name.trim().length() < 2) {
            throw new ValidationException("Customer name must be at least 2 characters long.");
        }
        if (!name.matches("^[a-zA-Z\\s.'-]+$")) {
            throw new ValidationException("Customer name contains invalid characters.");
        }
    }

    public static void validateEmail(String email) {
        if (email == null || !EMAIL_PATTERN.matcher(email.trim()).matches()) {
            throw new ValidationException("Invalid email format (example: user@domain.com).");
        }
    }

    public static void validatePositiveAmount(double amount, String fieldName) {
        if (amount <= 0.0) {
            throw new ValidationException(fieldName + " must be strictly positive (greater than 0.00).");
        }
        if (Double.isNaN(amount) || Double.isInfinite(amount)) {
            throw new ValidationException(fieldName + " must be a valid finite number.");
        }
    }

    public static void validateNonNegativeAmount(double amount, String fieldName) {
        if (amount < 0.0) {
            throw new ValidationException(fieldName + " cannot be negative.");
        }
    }

    public static AccountType validateAndParseAccountType(String typeStr) {
        if (typeStr == null || typeStr.trim().isEmpty()) {
            return AccountType.SAVINGS; // Default
        }
        String clean = typeStr.trim().toUpperCase().replace(" ", "_");
        try {
            return AccountType.valueOf(clean);
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Invalid account type: " + typeStr +
                    ". Valid types are: SAVINGS, CHECKING, FIXED_DEPOSIT.");
        }
    }
}
