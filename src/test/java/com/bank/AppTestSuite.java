package com.bank;

import com.bank.domain.*;
import com.bank.dsa.*;
import com.bank.exception.ValidationException;
import com.bank.service.BankService;
import com.bank.service.BankServiceImpl;
import com.bank.util.AnsiColor;
import com.bank.util.Validation;

import java.util.List;
import java.util.Optional;

/**
 * Standalone Test Suite that executes unit tests for all core DSA modules,
 * banking business logic, and granular input validation error handlers.
 */
public class AppTestSuite {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        System.out.println(AnsiColor.bold("\n======================================================="));
        System.out.println(AnsiColor.brightCyan("        RUNNING DSA & BANK SERVICE TEST SUITE"));
        System.out.println(AnsiColor.bold("=======================================================\n"));

        testEmailAndInputValidations();
        testTrie();
        testLRUCache();
        testBinarySearchTree();
        testMaxHeap();
        testMoneyFlowGraph();
        testTransactionUndoStack();
        testBankServiceOperations();

        System.out.println(AnsiColor.bold("\n======================================================="));
        System.out.println(String.format("TEST RESULTS: %s Passed | %s Failed",
                AnsiColor.green(String.valueOf(passed)),
                failed > 0 ? AnsiColor.red(String.valueOf(failed)) : AnsiColor.green("0")));
        System.out.println(AnsiColor.bold("=======================================================\n"));

        if (failed > 0) {
            System.exit(1);
        }
    }

    private static void test(String name, Runnable runnable) {
        try {
            runnable.run();
            passed++;
            System.out.println(AnsiColor.green("  ✔ [PASS] ") + name);
        } catch (Throwable t) {
            failed++;
            System.out.println(AnsiColor.red("  ✖ [FAIL] ") + name + " -> " + t.getMessage());
        }
    }

    private static void assertEquals(Object expected, Object actual, String msg) {
        if (!java.util.Objects.equals(expected, actual)) {
            throw new AssertionError(msg + " | Expected: " + expected + ", Got: " + actual);
        }
    }

    private static void assertTrue(boolean condition, String msg) {
        if (!condition) {
            throw new AssertionError(msg);
        }
    }

    private static void assertThrows(Class<? extends Throwable> expectedType, Runnable runnable, String msg) {
        try {
            runnable.run();
            throw new AssertionError("Expected " + expectedType.getSimpleName() + " was not thrown for: " + msg);
        } catch (Throwable t) {
            if (!expectedType.isInstance(t)) {
                throw new AssertionError("Wrong exception type thrown. Expected: " + expectedType.getSimpleName() +
                        ", Got: " + t.getClass().getSimpleName() + " (" + t.getMessage() + ")");
            }
        }
    }

    // =========================================================================
    // VALIDATION & ERROR HANDLING TESTS
    // =========================================================================

    private static void testEmailAndInputValidations() {
        System.out.println(AnsiColor.bold("[0] Granular Validation & Error Handling"));

        test("Valid email passes validation", () -> {
            Validation.validateEmail("user.name+tag@domain.com");
            Validation.validateEmail("alice@sub.bank.co.uk");
        });

        test("Email missing '@' throws ValidationException", () -> {
            assertThrows(ValidationException.class, () -> Validation.validateEmail("invalidemail.com"), "missing @");
        });

        test("Email with spaces throws ValidationException", () -> {
            assertThrows(ValidationException.class, () -> Validation.validateEmail("user name@domain.com"), "spaces");
        });

        test("Email missing domain throws ValidationException", () -> {
            assertThrows(ValidationException.class, () -> Validation.validateEmail("username@"), "missing domain");
        });

        test("Email missing username throws ValidationException", () -> {
            assertThrows(ValidationException.class, () -> Validation.validateEmail("@domain.com"), "missing username");
        });

        test("Email with single character TLD throws ValidationException", () -> {
            assertThrows(ValidationException.class, () -> Validation.validateEmail("user@domain.c"), "TLD too short");
        });

        test("Negative monetary amount throws ValidationException", () -> {
            assertThrows(ValidationException.class, () -> Validation.validatePositiveAmount(-50.0, "Deposit"), "negative amount");
            assertThrows(ValidationException.class, () -> Validation.validatePositiveAmount(0.0, "Deposit"), "zero amount");
        });

        test("Invalid phone number throws ValidationException", () -> {
            assertThrows(ValidationException.class, () -> Validation.validatePhone("123"), "too few digits");
            assertThrows(ValidationException.class, () -> Validation.validatePhone("abc-def-ghij"), "alphabetic characters");
        });

        test("Invalid account number format throws ValidationException", () -> {
            assertThrows(ValidationException.class, () -> Validation.validateAccountNumber("123456"), "missing prefix");
            assertThrows(ValidationException.class, () -> Validation.validateAccountNumber("XYZ-001"), "wrong prefix");
        });

        test("Invalid balance range (min > max) throws ValidationException", () -> {
            assertThrows(ValidationException.class, () -> Validation.validateBalanceRange(5000.0, 1000.0), "min > max");
        });
    }

    // =========================================================================
    // DSA TESTS
    // =========================================================================

    private static void testTrie() {
        System.out.println(AnsiColor.bold("\n[1] Trie (Prefix Tree)"));
        Trie<String> trie = new Trie<>();
        trie.insert("Alice Smith", "ALICE");
        trie.insert("Alexander Hamilton", "ALEX");
        trie.insert("Bob Builder", "BOB");

        test("Trie prefix search for 'Al' finds Alice and Alexander", () -> {
            List<String> results = trie.searchPrefix("al");
            assertEquals(2, results.size(), "Should match 2 prefixes");
            assertTrue(results.contains("ALICE"), "Should contain ALICE");
            assertTrue(results.contains("ALEX"), "Should contain ALEX");
        });

        test("Trie prefix search for token 'Hamilton'", () -> {
            List<String> results = trie.searchPrefix("Ham");
            assertEquals(1, results.size(), "Should match last name Hamilton");
            assertTrue(results.contains("ALEX"), "Should match ALEX");
        });

        test("Trie search for non-existent prefix returns empty", () -> {
            List<String> results = trie.searchPrefix("Zzz");
            assertEquals(0, results.size(), "Should be empty");
        });
    }

    private static void testLRUCache() {
        System.out.println(AnsiColor.bold("\n[2] LRU Cache"));
        LRUCache<String, Integer> cache = new LRUCache<>(3);
        cache.put("A", 1);
        cache.put("B", 2);
        cache.put("C", 3);

        test("LRU Cache get existing items", () -> {
            assertEquals(Optional.of(1), cache.get("A"), "Key A should exist");
            assertEquals(Optional.of(2), cache.get("B"), "Key B should exist");
        });

        test("LRU Cache evicts least recently accessed item when full", () -> {
            cache.put("D", 4); // Evicts C
            assertEquals(Optional.empty(), cache.get("C"), "Key C should be evicted");
            assertEquals(Optional.of(4), cache.get("D"), "Key D should exist");
            assertEquals(Optional.of(1), cache.get("A"), "Key A should still exist");
        });

        test("LRU Cache hit rate tracking", () -> {
            assertTrue(cache.getHitRatePercentage() > 0.0, "Hit rate should be positive");
            assertTrue(cache.getEvictionCount() >= 1, "Eviction count should be >= 1");
        });
    }

    private static void testBinarySearchTree() {
        System.out.println(AnsiColor.bold("\n[3] Binary Search Tree (Range Query)"));
        BinarySearchTree<Double, String> bst = new BinarySearchTree<>();
        bst.insert(100.0, "Account-100");
        bst.insert(500.0, "Account-500");
        bst.insert(1000.0, "Account-1000");
        bst.insert(2500.0, "Account-2500");
        bst.insert(5000.0, "Account-5000");

        test("BST Range Query [400 - 3000] finds middle accounts", () -> {
            List<String> range = bst.findRange(400.0, 3000.0);
            assertEquals(3, range.size(), "Should find 3 accounts in range");
            assertTrue(range.contains("Account-500"), "Must contain 500");
            assertTrue(range.contains("Account-1000"), "Must contain 1000");
            assertTrue(range.contains("Account-2500"), "Must contain 2500");
        });

        test("BST in-order traversal yields sorted elements", () -> {
            List<String> sorted = bst.inOrderTraversal();
            assertEquals(5, sorted.size(), "Total elements 5");
            assertEquals("Account-100", sorted.get(0), "First must be smallest");
            assertEquals("Account-5000", sorted.get(4), "Last must be largest");
        });
    }

    private static void testMaxHeap() {
        System.out.println(AnsiColor.bold("\n[4] Binary Max-Heap (Priority Queue)"));
        MaxHeap<ServiceRequest> heap = new MaxHeap<>();

        ServiceRequest reg = new ServiceRequest("C1", "Regular Cust", CustomerTier.REGULAR, "Issue 1");
        ServiceRequest gold = new ServiceRequest("C2", "Gold Cust", CustomerTier.GOLD, "Issue 2");
        ServiceRequest plat = new ServiceRequest("C3", "Platinum Cust", CustomerTier.PLATINUM, "Issue 3");

        heap.insert(reg);
        heap.insert(plat);
        heap.insert(gold);

        test("Max-Heap extracts Platinum VIP first, then Gold, then Regular", () -> {
            assertEquals(plat.getRequestId(), heap.extractMax().get().getRequestId(), "Platinum must be extracted 1st");
            assertEquals(gold.getRequestId(), heap.extractMax().get().getRequestId(), "Gold must be extracted 2nd");
            assertEquals(reg.getRequestId(), heap.extractMax().get().getRequestId(), "Regular must be extracted 3rd");
            assertTrue(heap.isEmpty(), "Heap should be empty after extracting all");
        });
    }

    private static void testMoneyFlowGraph() {
        System.out.println(AnsiColor.bold("\n[5] Money Flow Graph (AML & Shortest Path)"));
        MoneyFlowGraph graph = new MoneyFlowGraph();

        graph.recordTransfer("A", "B", 100.0, "TX1");
        graph.recordTransfer("B", "C", 100.0, "TX2");
        graph.recordTransfer("C", "D", 100.0, "TX3");

        test("Graph BFS finds shortest path from A to D", () -> {
            List<String> path = graph.findShortestTransferPath("A", "D");
            assertEquals(List.of("A", "B", "C", "D"), path, "Path must be A -> B -> C -> D");
        });

        test("Graph DFS detects circular AML ring (C -> A)", () -> {
            graph.recordTransfer("C", "A", 50.0, "TX4");
            List<List<String>> cycles = graph.detectCircularFlows("A");
            assertTrue(!cycles.isEmpty(), "Circular cycle must be detected");
            List<String> cycle = cycles.get(0);
            assertEquals("A", cycle.get(0), "Cycle starts with A");
            assertEquals("A", cycle.get(cycle.size() - 1), "Cycle ends with A");
        });
    }

    private static void testTransactionUndoStack() {
        System.out.println(AnsiColor.bold("\n[6] Transaction Audit Stack (Undo)"));
        TransactionAuditStack stack = new TransactionAuditStack();
        int[] counter = new int[]{10};

        stack.push("Increment", () -> counter[0] -= 5);

        test("Undo executes last command", () -> {
            counter[0] += 5;
            assertEquals(15, counter[0], "Counter updated to 15");
            Optional<TransactionAuditStack.ReversibleTransaction> op = stack.pop();
            assertTrue(op.isPresent(), "Stack should have operation");
            op.get().executeUndo();
            assertEquals(10, counter[0], "Counter reverted to 10");
        });
    }

    private static void testBankServiceOperations() {
        System.out.println(AnsiColor.bold("\n[7] Bank Service Financial Operations"));
        BankService service = new BankServiceImpl();

        String acc1 = service.openAccount("John Doe", "john@email.com", "555-1234", "SAVINGS", 1000.0, CustomerTier.SILVER);
        String acc2 = service.openAccount("Jane Smith", "jane@email.com", "555-5678", "CHECKING", 200.0, CustomerTier.GOLD);

        test("Deposit and balance update", () -> {
            service.deposit(acc1, 500.0, "Paycheck");
            assertEquals(1500.0, service.getAccount(acc1).getBalance(), "Balance should be 1500.0");
        });

        test("Transfer and balance integrity", () -> {
            service.transfer(acc1, acc2, 300.0, "Split bill");
            assertEquals(1200.0, service.getAccount(acc1).getBalance(), "Acc1 should be 1200.0");
            assertEquals(500.0, service.getAccount(acc2).getBalance(), "Acc2 should be 500.0");
        });

        test("Undo reverses the transfer", () -> {
            boolean undone = service.undoLastOperation();
            assertTrue(undone, "Undo should succeed");
            assertEquals(1500.0, service.getAccount(acc1).getBalance(), "Acc1 restored to 1500.0");
            assertEquals(200.0, service.getAccount(acc2).getBalance(), "Acc2 restored to 200.0");
        });
    }
}
