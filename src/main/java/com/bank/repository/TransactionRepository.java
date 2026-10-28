package com.bank.repository;

import com.bank.domain.Transaction;
import com.bank.dsa.MoneyFlowGraph;
import com.bank.dsa.TransactionAuditStack;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Repository for financial audit trails and transaction ledgers.
 * Integrates:
 * 1. Account Transaction Ledger (Map<String, List<Transaction>>).
 * 2. Directed Money Flow Graph (MoneyFlowGraph) for AML & shortest-path tracing.
 * 3. Reversal / Undo Stack (TransactionAuditStack) for operational rollbacks.
 */
public class TransactionRepository {

    private final Map<String, List<Transaction>> txByAccount = new HashMap<>();
    private final List<Transaction> allTransactions = new ArrayList<>();
    private final MoneyFlowGraph moneyFlowGraph = new MoneyFlowGraph();
    private final TransactionAuditStack auditStack = new TransactionAuditStack();

    /**
     * Persists a transaction in the account's ledger and the global ledger.
     */
    public synchronized void save(Transaction transaction) {
        if (transaction == null) return;

        allTransactions.add(transaction);
        txByAccount.computeIfAbsent(transaction.getAccountNumber(), k -> new ArrayList<>())
                   .add(transaction);

        // If transfer, record directed edge in the money flow graph
        if (transaction.getCounterpartAccount() != null) {
            moneyFlowGraph.recordTransfer(
                    transaction.getAccountNumber(),
                    transaction.getCounterpartAccount(),
                    transaction.getAmount(),
                    transaction.getTransactionId()
            );
        }
    }

    /**
     * Returns statement for an account ordered by timestamp.
     */
    public synchronized List<Transaction> findByAccountNumber(String accountNumber) {
        if (accountNumber == null) return Collections.emptyList();
        return txByAccount.getOrDefault(accountNumber, Collections.emptyList()).stream()
                .sorted(Comparator.comparing(Transaction::getTimestamp))
                .collect(Collectors.toList());
    }

    /**
     * Returns all transactions system-wide ordered by timestamp.
     */
    public synchronized List<Transaction> findAll() {
        return allTransactions.stream()
                .sorted(Comparator.comparing(Transaction::getTimestamp))
                .collect(Collectors.toList());
    }

    public synchronized MoneyFlowGraph getMoneyFlowGraph() {
        return moneyFlowGraph;
    }

    public synchronized TransactionAuditStack getAuditStack() {
        return auditStack;
    }

    public synchronized int count() {
        return allTransactions.size();
    }
}
