package com.bank.util;

import com.bank.domain.AccountType;
import com.bank.exception.ValidationException;

import java.util.regex.Pattern;

/**
 * Enterprise Validation Utility for banking inputs.
 * Implements granular, user-friendly error messages that pinpoint
 * exactly why an input (email, phone, name, amount) failed validation.
 */
public final class Validation {

    // RFC 5322 simplified pattern for robust email validation
    private static final Pattern STRICT_EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$"
    );

    // E.164 international phone number pattern (allows +, digits, spaces, hyphens, parens)
    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "^\\+?[0-9\\s()\\-.]{7,20}$"
    );

    private static final Pattern ACCOUNT_NUM_PATTERN = Pattern.compile(
            "^AC-[0-9]{4,8}$", Pattern.CASE_INSENSITIVE
    );

    private Validation() {}

    /**
     * Validates customer full name with granular error feedback.
     */
    public static void validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new ValidationException("Customer name cannot be empty or whitespace.");
        }
        String trimmed = name.trim();
        if (trimmed.length() < 2) {
            throw new ValidationException("Customer name is too short. Minimum length is 2 characters.");
        }
        if (trimmed.length() > 100) {
            throw new ValidationException("Customer name exceeds maximum allowed length (100 characters).");
        }
        if (!trimmed.matches("^[a-zA-Z\\s.'-]+$")) {
            throw new ValidationException(
                    "Customer name contains invalid characters. Only letters, spaces, hyphens, and apostrophes are allowed."
            );
        }
    }

    /**
     * Comprehensive, granular email validation.
     * Pinpoints the specific structural error to guide the user.
     */
    public static void validateEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new ValidationException("Email address cannot be empty or blank.");
        }
        String clean = email.trim();

        if (clean.contains(" ")) {
            throw new ValidationException("Email address cannot contain spaces.");
        }

        int atIndex = clean.indexOf('@');
        if (atIndex == -1) {
            throw new ValidationException("Invalid email format: missing '@' symbol (example: user@domain.com).");
        }

        if (clean.indexOf('@', atIndex + 1) != -1) {
            throw new ValidationException("Invalid email format: multiple '@' symbols found.");
        }

        String localPart = clean.substring(0, atIndex);
        String domainPart = clean.substring(atIndex + 1);

        if (localPart.isEmpty()) {
            throw new ValidationException("Invalid email format: missing username/local part before '@'.");
        }

        if (domainPart.isEmpty()) {
            throw new ValidationException("Invalid email format: missing domain name after '@' (e.g., domain.com).");
        }

        if (!domainPart.contains(".")) {
            throw new ValidationException("Invalid email domain: missing top-level domain (TLD) extension (e.g., .com, .org).");
        }

        int lastDot = domainPart.lastIndexOf('.');
        String tld = domainPart.substring(lastDot + 1);
        if (tld.length() < 2) {
            throw new ValidationException("Invalid email domain: TLD extension '." + tld + "' is too short (minimum 2 characters).");
        }

        if (!STRICT_EMAIL_PATTERN.matcher(clean).matches()) {
            throw new ValidationException("Invalid email address syntax: '" + clean + "'. Format must be user@domain.com.");
        }
    }

    /**
     * Validates customer phone numbers.
     */
    public static void validatePhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            throw new ValidationException("Phone number cannot be empty.");
        }
        String clean = phone.trim();
        // Count actual digits
        long digitCount = clean.chars().filter(Character::isDigit).count();
        if (digitCount < 7) {
            throw new ValidationException("Phone number must contain at least 7 digits.");
        }
        if (digitCount > 15) {
            throw new ValidationException("Phone number cannot exceed 15 digits (ITU-T E.164 standard).");
        }
        if (!PHONE_PATTERN.matcher(clean).matches()) {
            throw new ValidationException("Phone number contains invalid characters (allowed: +, digits, spaces, hyphens).");
        }
    }

    /**
     * Validates account numbers.
     */
    public static void validateAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.trim().isEmpty()) {
            throw new ValidationException("Account number cannot be empty or blank.");
        }
        String clean = accountNumber.trim().toUpperCase();
        if (!ACCOUNT_NUM_PATTERN.matcher(clean).matches()) {
            throw new ValidationException("Invalid account number format: '" + accountNumber +
                    "'. Must follow pattern AC-XXXXXX (e.g. AC-100001).");
        }
    }

    /**
     * Validates strictly positive monetary amounts.
     */
    public static void validatePositiveAmount(double amount, String fieldName) {
        if (Double.isNaN(amount) || Double.isInfinite(amount)) {
            throw new ValidationException(fieldName + " must be a valid finite numeric value.");
        }
        if (amount <= 0.0) {
            throw new ValidationException(fieldName + " must be strictly positive (greater than $0.00). Received: $" +
                    String.format("%.2f", amount));
        }
        if (amount > 100_000_000.0) {
            throw new ValidationException(fieldName + " exceeds the single transaction limit of $100,000,000.00.");
        }
    }

    /**
     * Validates non-negative monetary amounts (e.g. initial deposit).
     */
    public static void validateNonNegativeAmount(double amount, String fieldName) {
        if (Double.isNaN(amount) || Double.isInfinite(amount)) {
            throw new ValidationException(fieldName + " must be a valid finite numeric value.");
        }
        if (amount < 0.0) {
            throw new ValidationException(fieldName + " cannot be negative. Received: $" +
                    String.format("%.2f", amount));
        }
    }

    /**
     * Validates balance boundaries for range queries.
     */
    public static void validateBalanceRange(double minBalance, double maxBalance) {
        validateNonNegativeAmount(minBalance, "Minimum balance boundary");
        validateNonNegativeAmount(maxBalance, "Maximum balance boundary");
        if (minBalance > maxBalance) {
            throw new ValidationException(String.format(
                    "Invalid balance interval: minimum boundary ($%.2f) cannot exceed maximum boundary ($%.2f).",
                    minBalance, maxBalance
            ));
        }
    }

    /**
     * Validates descriptive text (notes, queries, descriptions).
     */
    public static void validateText(String text, String fieldName, int minLength, int maxLength) {
        if (text == null || text.trim().isEmpty()) {
            throw new ValidationException(fieldName + " cannot be empty or whitespace.");
        }
        String clean = text.trim();
        if (clean.length() < minLength) {
            throw new ValidationException(fieldName + " is too short (minimum " + minLength + " characters required).");
        }
        if (clean.length() > maxLength) {
            throw new ValidationException(fieldName + " exceeds maximum allowed length of " + maxLength + " characters.");
        }
    }

    /**
     * Validates and parses account type string.
     */
    public static AccountType validateAndParseAccountType(String typeStr) {
        if (typeStr == null || typeStr.trim().isEmpty()) {
            return AccountType.SAVINGS; // Default
        }
        String clean = typeStr.trim().toUpperCase().replace(" ", "_");
        try {
            return AccountType.valueOf(clean);
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Invalid account type: '" + typeStr +
                    "'. Valid types are: SAVINGS, CHECKING, FIXED_DEPOSIT.");
        }
    }
}
