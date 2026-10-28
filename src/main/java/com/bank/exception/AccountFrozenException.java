package com.bank.exception;

public class AccountFrozenException extends BankException {
    public AccountFrozenException(String accountNumber) {
        super("Account " + accountNumber + " is frozen or inactive. Transactions are disabled.");
    }
}
