package com.bank.service;

import com.bank.domain.Account;
import com.bank.domain.Customer;
import com.bank.domain.CustomerTier;
import com.bank.domain.ServiceRequest;
import com.bank.domain.Transaction;

import java.util.List;
import java.util.Optional;

/**
 * Core Service Interface orchestrating banking operations,
 * transactional integrity, and data structure interactions.
 */
public interface BankService {

    // --- Account Management ---
    String openAccount(String name, String email, String phone, String accountTypeStr,
                       double initialDeposit, CustomerTier tier);

    Account getAccount(String accountNumber);

    List<Account> listAllAccounts();

    boolean closeAccount(String accountNumber);

    // --- Financial Transactions ---
    Transaction deposit(String accountNumber, double amount, String note);

    Transaction withdraw(String accountNumber, double amount, String note);

    List<Transaction> transfer(String fromAccount, String toAccount, double amount, String note);

    boolean undoLastOperation();

    List<Transaction> getAccountStatement(String accountNumber);

    // --- DSA Search & Analytics ---
    List<Customer> searchCustomersByPrefix(String prefix);

    List<Account> findAccountsByBalanceRange(double minBalance, double maxBalance);

    List<Account> searchAccountsByCustomerName(String query);

    // --- Priority Customer Desk (Max-Heap) ---
    ServiceRequest submitServiceRequest(String customerId, String queryDetails);

    Optional<ServiceRequest> peekNextPriorityRequest();

    Optional<ServiceRequest> processNextPriorityRequest();

    List<ServiceRequest> listPendingServiceRequests();

    // --- AML & Graph Forensic Analysis (Graph BFS & DFS) ---
    List<String> traceMoneyTrail(String fromAccount, String toAccount);

    List<List<String>> detectSuspiciousCycles(String accountNumber);

    // --- Telemetry & Demo Data ---
    String getCacheDiagnostics();

    void seedDemoData();
}
