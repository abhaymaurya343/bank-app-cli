package com.bank.dsa;

import java.util.*;

/**
 * ============================================================================
 * DATA STRUCTURE: LRU (Least Recently Used) Cache
 * ============================================================================
 *
 * IMPLEMENTATION:
 * Built from scratch using a Doubly Linked List + HashMap combination.
 * - Doubly Linked List: Maintains access ordering in O(1).
 *   * Head: Most recently accessed element.
 *   * Tail: Least recently accessed element (eviction candidate).
 * - HashMap: Stores mapping of Key -> ListNode for O(1) direct address lookups.
 *
 * PURPOSE IN BANKING:
 * Bank transactions follow the Pareto Principle (80/20 rule): a small fraction of
 * active accounts receive the vast majority of deposits, balance inquiries, and transfers.
 * Rather than continuously fetching from disk or database, the Bank Service maintains
 * an in-memory LRU cache. When capacity is reached, the least-active account is evicted.
 *
 * TIME COMPLEXITY:
 * - get(K key):            O(1)
 * - put(K key, V value):   O(1)
 * - remove(K key):         O(1)
 *
 * SPACE COMPLEXITY:
 * - O(Capacity), bounded to prevent out-of-memory errors.
 *
 * @param <K> Key type (e.g. Account Number)
 * @param <V> Value type (e.g. Account entity)
 */
public class LRUCache<K, V> {

    /**
     * Doubly linked list node holding key and value.
     */
    private static class Node<K, V> {
        K key;
        V value;
        Node<K, V> prev;
        Node<K, V> next;

        Node(K key, V value) {
            this.key = key;
            this.value = value;
        }
    }

    private final int capacity;
    private final Map<K, Node<K, V>> map;
    private final Node<K, V> head; // Dummy sentinel head (most recent)
    private final Node<K, V> tail; // Dummy sentinel tail (least recent)

    // Performance telemetry
    private long hitCount = 0;
    private long missCount = 0;
    private long evictionCount = 0;

    public LRUCache(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Cache capacity must be greater than 0");
        }
        this.capacity = capacity;
        this.map = new HashMap<>(capacity);

        // Sentinel pseudo-nodes eliminate edge cases during insert/remove
        this.head = new Node<>(null, null);
        this.tail = new Node<>(null, null);
        head.next = tail;
        tail.prev = head;
    }

    /**
     * Retrieves an item from cache in O(1) time.
     * Moves the accessed item to the head (most recently used).
     */
    public synchronized Optional<V> get(K key) {
        Node<K, V> node = map.get(key);
        if (node == null) {
            missCount++;
            return Optional.empty();
        }
        hitCount++;
        moveToHead(node);
        return Optional.of(node.value);
    }

    /**
     * Stores an item into cache in O(1) time.
     * If key exists, updates value and moves to head.
     * If cache is full, evicts least recently used item from tail.
     */
    public synchronized void put(K key, V value) {
        if (key == null) return;

        Node<K, V> existingNode = map.get(key);
        if (existingNode != null) {
            existingNode.value = value;
            moveToHead(existingNode);
            return;
        }

        Node<K, V> newNode = new Node<>(key, value);
        map.put(key, newNode);
        addHead(newNode);

        if (map.size() > capacity) {
            Node<K, V> lru = removeTail();
            if (lru != null) {
                map.remove(lru.key);
                evictionCount++;
            }
        }
    }

    /**
     * Removes an item from the cache in O(1) time.
     */
    public synchronized boolean remove(K key) {
        Node<K, V> node = map.remove(key);
        if (node != null) {
            removeNode(node);
            return true;
        }
        return false;
    }

    /**
     * Returns all cached values ordered from Most Recently Used (MRU)
     * to Least Recently Used (LRU).
     */
    public synchronized List<V> getAllValuesOrdered() {
        List<V> list = new ArrayList<>(map.size());
        Node<K, V> curr = head.next;
        while (curr != tail) {
            list.add(curr.value);
            curr = curr.next;
        }
        return list;
    }

    /**
     * Clears all items and resets metrics.
     */
    public synchronized void clear() {
        map.clear();
        head.next = tail;
        tail.prev = head;
        hitCount = 0;
        missCount = 0;
        evictionCount = 0;
    }

    // --- Internal Doubly Linked List pointer manipulations (all O(1)) ---

    private void addHead(Node<K, V> node) {
        node.prev = head;
        node.next = head.next;
        head.next.prev = node;
        head.next = node;
    }

    private void removeNode(Node<K, V> node) {
        node.prev.next = node.next;
        node.next.prev = node.prev;
    }

    private void moveToHead(Node<K, V> node) {
        removeNode(node);
        addHead(node);
    }

    private Node<K, V> removeTail() {
        Node<K, V> lruNode = tail.prev;
        if (lruNode == head) {
            return null; // Cache is empty
        }
        removeNode(lruNode);
        return lruNode;
    }

    // --- Cache Telemetry & Diagnostics ---

    public int getCapacity() {
        return capacity;
    }

    public synchronized int size() {
        return map.size();
    }

    public synchronized long getHitCount() {
        return hitCount;
    }

    public synchronized long getMissCount() {
        return missCount;
    }

    public synchronized long getEvictionCount() {
        return evictionCount;
    }

    public synchronized double getHitRatePercentage() {
        long total = hitCount + missCount;
        if (total == 0) return 0.0;
        return (double) hitCount / total * 100.0;
    }

    public synchronized String getDiagnosticSummary() {
        return String.format(
            "LRU Cache Status: Size=%d/%d | Hits=%d | Misses=%d | Hit Rate=%.1f%% | Evictions=%d",
            size(), capacity, hitCount, missCount, getHitRatePercentage(), evictionCount
        );
    }
}
