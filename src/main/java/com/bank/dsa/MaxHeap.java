package com.bank.dsa;

import java.util.*;

/**
 * ============================================================================
 * DATA STRUCTURE: Binary Max-Heap (Priority Queue)
 * ============================================================================
 *
 * IMPLEMENTATION:
 * Array-backed complete binary tree satisfying the Max-Heap property:
 * for every node i, value(parent(i)) >= value(i).
 * - Root is always at index 0 and holds the MAXIMUM priority item.
 * - Left child of index i:   2*i + 1
 * - Right child of index i:  2*i + 2
 * - Parent of index i:       (i - 1) / 2
 *
 * PURPOSE IN BANKING:
 * Bank operations require rigorous triage:
 * 1. Customer Relationship Desk: High-tier VIP accounts (Platinum, Gold) must be
 *    serviced before regular accounts, even if regular accounts arrived earlier.
 * 2. Fraud & Security Monitoring: Transactions with high anomaly risk scores must
 *    be pushed to human fraud analysts immediately.
 * An array-based Max-Heap provides constant-time O(1) inspection of the highest-priority
 * item and logarithmic O(log N) insertion and extraction.
 *
 * TIME COMPLEXITY:
 * - peekMax():    O(1)
 * - insert():     O(log N) via siftUp
 * - extractMax(): O(log N) via siftDown
 * - size():       O(1)
 *
 * SPACE COMPLEXITY:
 * - O(N) contiguous memory.
 *
 * @param <T> Element type that implements Comparable
 */
public class MaxHeap<T extends Comparable<T>> {

    private final List<T> heap;

    public MaxHeap() {
        this.heap = new ArrayList<>();
    }

    public MaxHeap(int initialCapacity) {
        this.heap = new ArrayList<>(initialCapacity);
    }

    /**
     * Inserts an element into the Max-Heap.
     * Appends to end and sifts up to restore heap order.
     */
    public void insert(T item) {
        if (item == null) {
            throw new IllegalArgumentException("Cannot insert null into heap");
        }
        heap.add(item);
        siftUp(heap.size() - 1);
    }

    /**
     * Inspects the maximum priority element without removing it.
     */
    public Optional<T> peekMax() {
        if (heap.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(heap.get(0));
    }

    /**
     * Removes and returns the maximum priority element.
     * Replaces root with last element and sifts down.
     */
    public Optional<T> extractMax() {
        if (heap.isEmpty()) {
            return Optional.empty();
        }

        T maxItem = heap.get(0);
        T lastItem = heap.remove(heap.size() - 1);

        if (!heap.isEmpty()) {
            heap.set(0, lastItem);
            siftDown(0);
        }

        return Optional.of(maxItem);
    }

    /**
     * Restores max-heap property upwards from child to parent.
     */
    private void siftUp(int index) {
        while (index > 0) {
            int parentIdx = (index - 1) / 2;
            T current = heap.get(index);
            T parent = heap.get(parentIdx);

            if (current.compareTo(parent) > 0) {
                // Current is greater than parent; swap them
                swap(index, parentIdx);
                index = parentIdx;
            } else {
                break;
            }
        }
    }

    /**
     * Restores max-heap property downwards from parent to children.
     */
    private void siftDown(int index) {
        int size = heap.size();
        while (index < size) {
            int leftChildIdx = 2 * index + 1;
            int rightChildIdx = 2 * index + 2;
            int largestIdx = index;

            if (leftChildIdx < size && heap.get(leftChildIdx).compareTo(heap.get(largestIdx)) > 0) {
                largestIdx = leftChildIdx;
            }

            if (rightChildIdx < size && heap.get(rightChildIdx).compareTo(heap.get(largestIdx)) > 0) {
                largestIdx = rightChildIdx;
            }

            if (largestIdx != index) {
                swap(index, largestIdx);
                index = largestIdx;
            } else {
                break;
            }
        }
    }

    private void swap(int i, int j) {
        T temp = heap.get(i);
        heap.set(i, heap.get(j));
        heap.set(j, temp);
    }

    public int size() {
        return heap.size();
    }

    public boolean isEmpty() {
        return heap.isEmpty();
    }

    /**
     * Returns a snapshot list of items in the heap.
     */
    public List<T> toList() {
        return new ArrayList<>(heap);
    }

    public void clear() {
        heap.clear();
    }
}
