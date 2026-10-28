package com.bank.dsa;

import java.util.*;

/**
 * ============================================================================
 * DATA STRUCTURE: Trie (Prefix Tree)
 * ============================================================================
 *
 * PURPOSE IN BANKING:
 * In a real-world bank with millions of customers, performing a linear scan (O(N))
 * on every customer's name for instant search / autocomplete causes high CPU load.
 * A Trie indexes names character-by-character, enabling sub-millisecond prefix
 * lookups proportional ONLY to the length of the query string, independent of
 * how many millions of customer records exist.
 *
 * TIME COMPLEXITY:
 * - Insertion:     O(L), where L is the length of the string key.
 * - Prefix Search: O(P + K), where P is the length of the prefix and K is the
 *                  number of matching results collected.
 * - Exact Match:   O(L).
 *
 * SPACE COMPLEXITY:
 * - O(ALPHABET_SIZE * L * N) in worst case, but shared prefixes optimize storage.
 *
 * @param <V> The payload type stored at nodes (e.g., Customer or Account)
 */
public class Trie<V> {

    /**
     * Internal node representing a single character and holding payloads.
     */
    private static class TrieNode<V> {
        // Map child character to next node (case-insensitive)
        final Map<Character, TrieNode<V>> children = new HashMap<>();
        // Values associated with words ending at or passing through this node
        final Set<V> values = new HashSet<>();
        boolean isEndOfWord = false;
    }

    private final TrieNode<V> root;
    private int size; // Number of unique keys inserted

    public Trie() {
        this.root = new TrieNode<>();
        this.size = 0;
    }

    /**
     * Inserts a key and associates a value with it.
     * All tokens in multi-word names (e.g. "Alice Smith" -> "Alice", "Smith")
     * can be indexed so searching either first or last name succeeds.
     *
     * @param key   The string to index (e.g., customer name)
     * @param value The object to store (e.g., Customer object)
     */
    public void insert(String key, V value) {
        if (key == null || key.trim().isEmpty() || value == null) {
            return;
        }

        // Insert full string
        insertSingleToken(key.trim().toLowerCase(), value);

        // Also index individual space-separated tokens for first/last name lookup
        String[] tokens = key.trim().toLowerCase().split("\\s+");
        if (tokens.length > 1) {
            for (String token : tokens) {
                if (!token.isEmpty()) {
                    insertSingleToken(token, value);
                }
            }
        }
    }

    private void insertSingleToken(String token, V value) {
        TrieNode<V> current = root;
        for (char ch : token.toCharArray()) {
            current = current.children.computeIfAbsent(ch, c -> new TrieNode<>());
            current.values.add(value);
        }
        if (!current.isEndOfWord) {
            current.isEndOfWord = true;
            size++;
        }
    }

    /**
     * Searches for all values whose keys start with the given prefix.
     *
     * @param prefix Prefix query (e.g. "ali")
     * @return List of matching values (e.g. all customers starting with "ali")
     */
    public List<V> searchPrefix(String prefix) {
        if (prefix == null || prefix.trim().isEmpty()) {
            return Collections.emptyList();
        }

        String normalized = prefix.trim().toLowerCase();
        TrieNode<V> current = root;

        for (char ch : normalized.toCharArray()) {
            current = current.children.get(ch);
            if (current == null) {
                return Collections.emptyList(); // Prefix not found
            }
        }

        // The values set at the prefix node already contains all values in the subtree
        return new ArrayList<>(current.values);
    }

    /**
     * Checks if an exact key exists in the Trie.
     */
    public boolean containsExact(String key) {
        if (key == null || key.trim().isEmpty()) return false;
        String normalized = key.trim().toLowerCase();
        TrieNode<V> current = root;
        for (char ch : normalized.toCharArray()) {
            current = current.children.get(ch);
            if (current == null) return false;
        }
        return current.isEndOfWord;
    }

    public int getSize() {
        return size;
    }

    public void clear() {
        root.children.clear();
        root.values.clear();
        size = 0;
    }
}
