package com.bank.exception;

public class InsufficientFundsException extends BankException {
    public InsufficientFundsException(String message) {
        super(message);
    }

    public InsufficientFundsException(String accountNumber, double currentBalance, double requestedAmount) {
        super(String.format("Insufficient funds in account %s. Current balance: $%.2f, Requested amount: $%.2f",
                accountNumber, currentBalance, requestedAmount));
    }
}
