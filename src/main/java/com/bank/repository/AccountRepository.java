package com.bank.repository;

import com.bank.domain.Account;
import com.bank.dsa.BinarySearchTree;
import com.bank.dsa.LRUCache;

import java.util.*;
import java.util.stream.Collectors;

/**
 * High-performance Repository for Account entities.
 * Integrates:
 * 1. Primary Hash Index (Map<String, Account>) for O(1) direct ID lookups.
 * 2. L1 High-Speed LRU Cache (LRUCache<String, Account>) for hot-account caching.
 * 3. Range-Search Balance Index (BinarySearchTree<Double, Account>) for O(log N + K) queries.
 */
public class AccountRepository {

    private final Map<String, Account> accountsByNumber = new HashMap<>();
    private final LRUCache<String, Account> accountCache;
    private final BinarySearchTree<Double, Account> balanceTree = new BinarySearchTree<>();

    public AccountRepository(int cacheCapacity) {
        this.accountCache = new LRUCache<>(cacheCapacity);
    }

    public AccountRepository() {
        this(50); // Default cache capacity of 50 accounts
    }

    /**
     * Persists or updates an account.
     * Synchronizes hash map, updates LRU cache, and indexes in balance BST.
     */
    public synchronized void save(Account account) {
        if (account == null) return;

        Account existing = accountsByNumber.get(account.getAccountNumber());
        if (existing != null) {
            // Remove old balance from tree before updating
            balanceTree.remove(existing.getBalance(), existing);
        }

        accountsByNumber.put(account.getAccountNumber(), account);
        accountCache.put(account.getAccountNumber(), account);
        balanceTree.insert(account.getBalance(), account);
    }

    /**
     * Finds an account by account number.
     * Demonstrates L1 Cache Hit / Miss behavior:
     * - Checks LRUCache first: if present, returns immediately (Cache Hit).
     * - If not present: fetches from accountsByNumber, populates cache, returns (Cache Miss).
     */
    public synchronized Optional<Account> findByNumber(String accountNumber) {
        if (accountNumber == null) return Optional.empty();

        // 1. Check L1 Cache
        Optional<Account> cached = accountCache.get(accountNumber);
        if (cached.isPresent()) {
            return cached;
        }

        // 2. Fetch from Primary Map
        Account account = accountsByNumber.get(accountNumber);
        if (account != null) {
            // Populate L1 Cache
            accountCache.put(accountNumber, account);
            return Optional.of(account);
        }

        return Optional.empty();
    }

    /**
     * Returns all accounts sorted by Account Number.
     */
    public synchronized List<Account> findAll() {
        return accountsByNumber.values().stream()
                .sorted(Comparator.comparing(Account::getAccountNumber))
                .collect(Collectors.toList());
    }

    /**
     * Returns all accounts associated with a customer ID.
     */
    public synchronized List<Account> findByCustomerId(String customerId) {
        if (customerId == null) return Collections.emptyList();
        return accountsByNumber.values().stream()
                .filter(a -> customerId.equals(a.getCustomerId()))
                .sorted(Comparator.comparing(Account::getAccountNumber))
                .collect(Collectors.toList());
    }

    /**
     * Queries accounts within balance interval [minBalance, maxBalance].
     * Uses Binary Search Tree range search: O(log N + K).
     */
    public synchronized List<Account> findByBalanceRange(double minBalance, double maxBalance) {
        return balanceTree.findRange(minBalance, maxBalance);
    }

    public synchronized LRUCache<String, Account> getCache() {
        return accountCache;
    }

    public synchronized BinarySearchTree<Double, Account> getBalanceTree() {
        return balanceTree;
    }

    public synchronized int count() {
        return accountsByNumber.size();
    }
}
