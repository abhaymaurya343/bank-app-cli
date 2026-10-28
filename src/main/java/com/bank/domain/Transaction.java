package com.bank.domain;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.UUID;

/**
 * Transaction domain model representing an immutable audit log entry.
 */
public class Transaction implements Comparable<Transaction> {
    private final String transactionId;
    private final String accountNumber;
    private final String counterpartAccount; // For transfers, other account; otherwise null
    private final double amount;
    private final TransactionType type;
    private final String note;
    private final double balanceAfter;
    private final LocalDateTime timestamp;

    public Transaction(String transactionId, String accountNumber, String counterpartAccount,
                       double amount, TransactionType type, String note, double balanceAfter,
                       LocalDateTime timestamp) {
        this.transactionId = transactionId != null ? transactionId : "TX-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        this.accountNumber = accountNumber;
        this.counterpartAccount = counterpartAccount;
        this.amount = amount;
        this.type = type;
        this.note = note != null ? note : "";
        this.balanceAfter = balanceAfter;
        this.timestamp = timestamp != null ? timestamp : LocalDateTime.now();
    }

    public Transaction(String accountNumber, String counterpartAccount, double amount,
                       TransactionType type, String note, double balanceAfter) {
        this(null, accountNumber, counterpartAccount, amount, type, note, balanceAfter, LocalDateTime.now());
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public String getCounterpartAccount() {
        return counterpartAccount;
    }

    public double getAmount() {
        return amount;
    }

    public TransactionType getType() {
        return type;
    }

    public String getNote() {
        return note;
    }

    public double getBalanceAfter() {
        return balanceAfter;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getFormattedTimestamp() {
        return timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    @Override
    public int compareTo(Transaction other) {
        if (other == null) return 1;
        int timeCmp = this.timestamp.compareTo(other.timestamp);
        if (timeCmp != 0) return timeCmp;
        return this.transactionId.compareTo(other.transactionId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Transaction that = (Transaction) o;
        return Objects.equals(transactionId, that.transactionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(transactionId);
    }

    @Override
    public String toString() {
        return String.format("Transaction[%s | %s | %s | %s$%.2f | Bal: $%.2f | %s]",
                transactionId, getFormattedTimestamp(), type, type.getSign(), amount, balanceAfter, note);
    }
}
