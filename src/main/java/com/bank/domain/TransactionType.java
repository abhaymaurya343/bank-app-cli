package com.bank.domain;

/**
 * Types of financial operations supported in the banking system.
 */
public enum TransactionType {
    DEPOSIT("Deposit", "+"),
    WITHDRAWAL("Withdrawal", "-"),
    TRANSFER_OUT("Transfer Out", "-"),
    TRANSFER_IN("Transfer In", "+"),
    REVERSAL("Transaction Reversal", "+/-");

    private final String description;
    private final String sign;

    TransactionType(String description, String sign) {
        this.description = description;
        this.sign = sign;
    }

    public String getDescription() {
        return description;
    }

    public String getSign() {
        return sign;
    }
}
