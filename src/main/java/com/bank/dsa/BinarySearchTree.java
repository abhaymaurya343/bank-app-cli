package com.bank.dsa;

import java.util.*;

/**
 * ============================================================================
 * DATA STRUCTURE: Binary Search Tree (BST) with Range Query Support
 * ============================================================================
 *
 * IMPLEMENTATION:
 * Binary Search Tree supporting duplicate keys by maintaining a bucket (List<V>)
 * at each node. For example, if multiple accounts hold an identical balance of $1,000,
 * all those accounts reside in the node corresponding to key 1000.0.
 *
 * PURPOSE IN BANKING:
 * Regulatory compliance, risk assessments, and targeted customer segmentations
 * require frequent range inquiries:
 * "Find all accounts with balance between $5,000 and $25,000" or
 * "List high-net-worth accounts exceeding $100,000".
 * A linear scan over a relational table is O(N). By indexing balances inside a
 * Binary Search Tree, range queries prune entire branches that fall outside the
 * boundary, executing in O(log N + K) time where K is the number of matched records.
 *
 * TIME COMPLEXITY:
 * - Insert:       O(log N) average, O(N) worst-case (unbalanced).
 * - Delete:       O(log N) average.
 * - Range Query:  O(log N + K) average, where K is number of items in range.
 * - Min / Max:    O(log N) average.
 * - In-Order:     O(N) to retrieve all records in sorted balance order.
 *
 * @param <K> Comparable key (e.g. Double balance)
 * @param <V> Payload value (e.g. Account entity)
 */
public class BinarySearchTree<K extends Comparable<K>, V> {

    /**
     * Internal BST Node holding key and duplicate values list.
     */
    private static class Node<K, V> {
        K key;
        final List<V> values = new ArrayList<>();
        Node<K, V> left;
        Node<K, V> right;

        Node(K key, V value) {
            this.key = key;
            this.values.add(value);
        }
    }

    private Node<K, V> root;
    private int size = 0;

    public BinarySearchTree() {
        this.root = null;
    }

    /**
     * Inserts an item indexed by the given key.
     */
    public void insert(K key, V value) {
        if (key == null || value == null) return;
        root = insertRecursive(root, key, value);
        size++;
    }

    private Node<K, V> insertRecursive(Node<K, V> current, K key, V value) {
        if (current == null) {
            return new Node<>(key, value);
        }

        int cmp = key.compareTo(current.key);
        if (cmp < 0) {
            current.left = insertRecursive(current.left, key, value);
        } else if (cmp > 0) {
            current.right = insertRecursive(current.right, key, value);
        } else {
            // Duplicate key: add to existing node bucket
            current.values.add(value);
        }
        return current;
    }

    /**
     * Removes a specific item from the tree under the given key.
     */
    public boolean remove(K key, V value) {
        if (key == null || value == null) return false;
        int initialSize = size;
        root = removeRecursive(root, key, value);
        return size < initialSize;
    }

    private Node<K, V> removeRecursive(Node<K, V> current, K key, V value) {
        if (current == null) return null;

        int cmp = key.compareTo(current.key);
        if (cmp < 0) {
            current.left = removeRecursive(current.left, key, value);
        } else if (cmp > 0) {
            current.right = removeRecursive(current.right, key, value);
        } else {
            // Found matching key node
            if (current.values.remove(value)) {
                size--;
            }
            // If values remain with this key, preserve node
            if (!current.values.isEmpty()) {
                return current;
            }

            // Node bucket is empty; standard BST node deletion
            if (current.left == null) return current.right;
            if (current.right == null) return current.left;

            // Node with two children: replace with in-order successor (min of right subtree)
            Node<K, V> successor = findMinNode(current.right);
            current.key = successor.key;
            current.values.clear();
            current.values.addAll(successor.values);
            current.right = removeAllKeyRecursive(current.right, successor.key);
        }
        return current;
    }

    private Node<K, V> removeAllKeyRecursive(Node<K, V> current, K key) {
        if (current == null) return null;
        int cmp = key.compareTo(current.key);
        if (cmp < 0) {
            current.left = removeAllKeyRecursive(current.left, key);
        } else if (cmp > 0) {
            current.right = removeAllKeyRecursive(current.right, key);
        } else {
            // Delete this node
            if (current.left == null) return current.right;
            if (current.right == null) return current.left;
            Node<K, V> successor = findMinNode(current.right);
            current.key = successor.key;
            current.values.clear();
            current.values.addAll(successor.values);
            current.right = removeAllKeyRecursive(current.right, successor.key);
        }
        return current;
    }

    private Node<K, V> findMinNode(Node<K, V> node) {
        Node<K, V> curr = node;
        while (curr.left != null) {
            curr = curr.left;
        }
        return curr;
    }

    /**
     * Finds all elements within the closed interval [min, max].
     * Prunes subtrees completely outside the range for optimal O(log N + K) performance.
     */
    public List<V> findRange(K min, K max) {
        List<V> results = new ArrayList<>();
        rangeSearchRecursive(root, min, max, results);
        return results;
    }

    private void rangeSearchRecursive(Node<K, V> current, K min, K max, List<V> results) {
        if (current == null) return;

        // If current key is greater than min, left subtree might have qualifying elements
        if (min == null || current.key.compareTo(min) > 0) {
            rangeSearchRecursive(current.left, min, max, results);
        }

        // If current key lies inside [min, max], collect its values
        boolean withinLower = (min == null || current.key.compareTo(min) >= 0);
        boolean withinUpper = (max == null || current.key.compareTo(max) <= 0);
        if (withinLower && withinUpper) {
            results.addAll(current.values);
        }

        // If current key is less than max, right subtree might have qualifying elements
        if (max == null || current.key.compareTo(max) < 0) {
            rangeSearchRecursive(current.right, min, max, results);
        }
    }

    /**
     * Returns all elements sorted in ascending order of their key via in-order traversal.
     */
    public List<V> inOrderTraversal() {
        List<V> results = new ArrayList<>(size);
        inOrderRecursive(root, results);
        return results;
    }

    private void inOrderRecursive(Node<K, V> current, List<V> results) {
        if (current == null) return;
        inOrderRecursive(current.left, results);
        results.addAll(current.values);
        inOrderRecursive(current.right, results);
    }

    public int getSize() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public void clear() {
        root = null;
        size = 0;
    }
}
