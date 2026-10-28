package com.bank.domain;

/**
 * Represents the type of bank account and its business rules.
 */
public enum AccountType {
    SAVINGS("Savings Account", 100.00, 0.04),
    CHECKING("Checking Account", 0.00, 0.00),
    FIXED_DEPOSIT("Fixed Deposit", 500.00, 0.07);

    private final String displayName;
    private final double minimumBalance;
    private final double annualInterestRate;

    AccountType(String displayName, double minimumBalance, double annualInterestRate) {
        this.displayName = displayName;
        this.minimumBalance = minimumBalance;
        this.annualInterestRate = annualInterestRate;
    }

    public String getDisplayName() {
        return displayName;
    }

    public double getMinimumBalance() {
        return minimumBalance;
    }

    public double getAnnualInterestRate() {
        return annualInterestRate;
    }
}
