package com.bank.dsa;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;

/**
 * ============================================================================
 * DATA STRUCTURE: Stack for Transaction Undo / Rollback (LIFO)
 * ============================================================================
 *
 * IMPLEMENTATION:
 * ArrayDeque-backed Last-In, First-Out (LIFO) Stack storing executable
 * reversible operations (Command Pattern).
 *
 * PURPOSE IN BANKING:
 * Bank operations occasionally require immediate teller reversal / rollback
 * (e.g., teller entered $1000 instead of $100, or a duplicate transfer was triggered).
 * A Stack records state transitions so the most recent operation (TOS - Top Of Stack)
 * can be undone in reverse chronological order:
 * - Undo Deposit: Deducts deposited funds and logs reversal transaction.
 * - Undo Transfer: Returns funds from receiver back to sender and logs reversals.
 *
 * TIME COMPLEXITY:
 * - push(): O(1)
 * - pop():  O(1)
 * - peek(): O(1)
 */
public class TransactionAuditStack {

    /**
     * Functional command representing an operation that can be rolled back.
     */
    @FunctionalInterface
    public interface ReversibleAction {
        void undo();
    }

    public static class ReversibleTransaction {
        private final String description;
        private final ReversibleAction action;

        public ReversibleTransaction(String description, ReversibleAction action) {
            this.description = description;
            this.action = action;
        }

        public String getDescription() {
            return description;
        }

        public void executeUndo() {
            action.undo();
        }
    }

    private final Deque<ReversibleTransaction> stack = new ArrayDeque<>();

    public synchronized void push(String description, ReversibleAction action) {
        stack.push(new ReversibleTransaction(description, action));
    }

    public synchronized Optional<ReversibleTransaction> pop() {
        if (stack.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(stack.pop());
    }

    public synchronized Optional<ReversibleTransaction> peek() {
        if (stack.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(stack.peek());
    }

    public synchronized int size() {
        return stack.size();
    }

    public synchronized boolean isEmpty() {
        return stack.isEmpty();
    }

    public synchronized void clear() {
        stack.clear();
    }
}
