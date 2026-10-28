package com.bank.domain;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Account domain model representing a financial account.
 */
public class Account implements Comparable<Account> {
    private final String accountNumber;
    private final String customerId;
    private String customerName;
    private final AccountType accountType;
    private double balance;
    private boolean active;
    private final LocalDateTime createdAt;

    public Account(String accountNumber, String customerId, String customerName, AccountType accountType, double initialBalance) {
        this.accountNumber = accountNumber;
        this.customerId = customerId;
        this.customerName = customerName;
        this.accountType = accountType != null ? accountType : AccountType.SAVINGS;
        this.balance = initialBalance;
        this.active = true;
        this.createdAt = LocalDateTime.now();
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public AccountType getAccountType() {
        return accountType;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    public int compareTo(Account other) {
        if (other == null) return 1;
        // Natural ordering by balance, secondary ordering by account number
        int balanceCmp = Double.compare(this.balance, other.balance);
        if (balanceCmp != 0) return balanceCmp;
        return this.accountNumber.compareTo(other.accountNumber);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Account account = (Account) o;
        return Objects.equals(accountNumber, account.accountNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accountNumber);
    }

    @Override
    public String toString() {
        return String.format("Account[num=%s, holder='%s', type=%s, balance=$%.2f, active=%s]",
                accountNumber, customerName, accountType, balance, active);
    }
}
