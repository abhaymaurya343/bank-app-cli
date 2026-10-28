package com.bank.repository;

import com.bank.domain.Customer;
import com.bank.dsa.Trie;

import java.util.*;
import java.util.stream.Collectors;

/**
 * High-performance Repository for Customer entities.
 * Integrates:
 * 1. Primary Hash Index (Map<String, Customer>) for O(1) customer ID lookup.
 * 2. Prefix Search Index (Trie<Customer>) for O(P + K) sub-millisecond name search.
 */
public class CustomerRepository {

    private final Map<String, Customer> customersById = new HashMap<>();
    private final Trie<Customer> nameTrie = new Trie<>();

    /**
     * Persists customer and indexes their name into the Prefix Trie.
     */
    public synchronized void save(Customer customer) {
        if (customer == null) return;
        customersById.put(customer.getCustomerId(), customer);
        nameTrie.insert(customer.getName(), customer);
    }

    public synchronized Optional<Customer> findById(String customerId) {
        if (customerId == null) return Optional.empty();
        return Optional.ofNullable(customersById.get(customerId));
    }

    public synchronized Optional<Customer> findByEmail(String email) {
        if (email == null) return Optional.empty();
        return customersById.values().stream()
                .filter(c -> email.equalsIgnoreCase(c.getEmail()))
                .findFirst();
    }

    public synchronized List<Customer> findAll() {
        return customersById.values().stream()
                .sorted(Comparator.comparing(Customer::getName))
                .collect(Collectors.toList());
    }

    /**
     * Fast Prefix Search using the Trie.
     * Searches customers whose first or last names start with prefix.
     * Execution time is O(P + K) rather than O(N).
     */
    public synchronized List<Customer> searchByNamePrefix(String prefix) {
        return nameTrie.searchPrefix(prefix).stream()
                .distinct()
                .sorted(Comparator.comparing(Customer::getName))
                .collect(Collectors.toList());
    }

    public synchronized Trie<Customer> getNameTrie() {
        return nameTrie;
    }

    public synchronized int count() {
        return customersById.size();
    }
}
