package com.bank.dsa;

import java.time.LocalDateTime;
import java.util.*;

/**
 * ============================================================================
 * DATA STRUCTURE & ALGORITHMS: Directed Money Flow Graph (AML Analysis)
 * ============================================================================
 *
 * IMPLEMENTATION:
 * Directed graph with adjacency list representation:
 * - Vertices: Account Numbers (String)
 * - Directed Edges: Transfers from Source Account -> Target Account with Amount & Timestamp.
 *
 * PURPOSE IN BANKING:
 * 1. Money Trail Tracking (BFS):
 *    When forensic investigators track stolen or illicit funds, they need to find
 *    the shortest transfer path connecting an originating account to an endpoint account.
 *    Breadth-First Search (BFS) finds the minimum-hop transfer chain in O(V + E) time.
 *
 * 2. Anti-Money Laundering (AML) Circular Ring Detection (DFS):
 *    A classic money-laundering technique ("round-tripping" / "smurfing") routes
 *    money through multiple mule accounts in a cycle (e.g., A -> B -> C -> A) to
 *    disguise the source of funds.
 *    Using Depth-First Search (DFS) with a recursion stack, we detect cycles
 *    originating from or involving a suspect account.
 *
 * TIME COMPLEXITY:
 * - Add Transfer Edge: O(1)
 * - Shortest Path BFS: O(V + E)
 * - Cycle Detection DFS: O(V + E)
 *
 * SPACE COMPLEXITY:
 * - O(V + E) for adjacency list, visited sets, and recursion stacks.
 */
public class MoneyFlowGraph {

    /**
     * Directed edge representing a transfer transaction.
     */
    public static class TransferEdge {
        private final String fromAccount;
        private final String toAccount;
        private final double amount;
        private final LocalDateTime timestamp;
        private final String transactionId;

        public TransferEdge(String fromAccount, String toAccount, double amount, String transactionId) {
            this.fromAccount = fromAccount;
            this.toAccount = toAccount;
            this.amount = amount;
            this.transactionId = transactionId;
            this.timestamp = LocalDateTime.now();
        }

        public String getFromAccount() {
            return fromAccount;
        }

        public String getToAccount() {
            return toAccount;
        }

        public double getAmount() {
            return amount;
        }

        public LocalDateTime getTimestamp() {
            return timestamp;
        }

        public String getTransactionId() {
            return transactionId;
        }

        @Override
        public String toString() {
            return String.format("%s -> %s ($%.2f)", fromAccount, toAccount, amount);
        }
    }

    private final Map<String, List<TransferEdge>> adjacencyList = new HashMap<>();

    /**
     * Adds an account node to the graph if not already present.
     */
    public synchronized void addAccount(String accountNumber) {
        if (accountNumber != null) {
            adjacencyList.computeIfAbsent(accountNumber, k -> new ArrayList<>());
        }
    }

    /**
     * Records a directed fund transfer edge in the graph.
     */
    public synchronized void recordTransfer(String fromAccount, String toAccount, double amount, String txId) {
        if (fromAccount == null || toAccount == null || fromAccount.equalsIgnoreCase(toAccount)) {
            return;
        }
        addAccount(fromAccount);
        addAccount(toAccount);

        TransferEdge edge = new TransferEdge(fromAccount, toAccount, amount, txId);
        adjacencyList.get(fromAccount).add(edge);
    }

    /**
     * ALGORITHM: Breadth-First Search (BFS)
     * Finds the shortest path of transfer hops connecting startAccount to targetAccount.
     *
     * @param startAccount  Source account number
     * @param targetAccount Destination account number
     * @return List of account numbers forming the transfer path, or empty if disconnected
     */
    public synchronized List<String> findShortestTransferPath(String startAccount, String targetAccount) {
        if (startAccount == null || targetAccount == null) return Collections.emptyList();
        if (!adjacencyList.containsKey(startAccount) || !adjacencyList.containsKey(targetAccount)) {
            return Collections.emptyList();
        }
        if (startAccount.equalsIgnoreCase(targetAccount)) {
            return Collections.singletonList(startAccount);
        }

        Queue<String> queue = new ArrayDeque<>();
        Set<String> visited = new HashSet<>();
        Map<String, String> parentMap = new HashMap<>();

        queue.add(startAccount);
        visited.add(startAccount);

        boolean found = false;

        while (!queue.isEmpty()) {
            String current = queue.poll();
            if (current.equalsIgnoreCase(targetAccount)) {
                found = true;
                break;
            }

            List<TransferEdge> edges = adjacencyList.getOrDefault(current, Collections.emptyList());
            for (TransferEdge edge : edges) {
                String neighbor = edge.getToAccount();
                if (!visited.contains(neighbor)) {
                    visited.add(neighbor);
                    parentMap.put(neighbor, current);
                    queue.add(neighbor);
                }
            }
        }

        if (!found) {
            return Collections.emptyList(); // No path found
        }

        // Reconstruct path backwards from targetAccount to startAccount
        LinkedList<String> path = new LinkedList<>();
        String curr = targetAccount;
        while (curr != null) {
            path.addFirst(curr);
            curr = parentMap.get(curr);
        }

        return path;
    }

    /**
     * ALGORITHM: Depth-First Search (DFS) Cycle Detection
     * Anti-Money Laundering (AML) check: Detects if funds originating from startAccount
     * loop back to startAccount through intermediate accounts (e.g. A -> B -> C -> A).
     *
     * @param startAccount Account to audit
     * @return List of detected cycle paths (each path is an ordered list of accounts)
     */
    public synchronized List<List<String>> detectCircularFlows(String startAccount) {
        if (startAccount == null || !adjacencyList.containsKey(startAccount)) {
            return Collections.emptyList();
        }

        List<List<String>> detectedCycles = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        List<String> currentPath = new ArrayList<>();

        currentPath.add(startAccount);
        dfsDetectCycle(startAccount, startAccount, visited, currentPath, detectedCycles, 0);

        return detectedCycles;
    }

    private void dfsDetectCycle(String startAccount, String currentAccount, Set<String> visited,
                                List<String> currentPath, List<List<String>> detectedCycles, int depth) {
        // Prevent infinite exploration in dense networks
        if (depth > 10) return;

        List<TransferEdge> outgoing = adjacencyList.getOrDefault(currentAccount, Collections.emptyList());

        for (TransferEdge edge : outgoing) {
            String neighbor = edge.getToAccount();

            if (neighbor.equalsIgnoreCase(startAccount) && currentPath.size() > 1) {
                // Cycle closed back to startAccount!
                List<String> cycleFound = new ArrayList<>(currentPath);
                cycleFound.add(startAccount);
                detectedCycles.add(cycleFound);
            } else if (!visited.contains(neighbor) && !currentPath.contains(neighbor)) {
                visited.add(neighbor);
                currentPath.add(neighbor);

                dfsDetectCycle(startAccount, neighbor, visited, currentPath, detectedCycles, depth + 1);

                // Backtrack
                currentPath.remove(currentPath.size() - 1);
                visited.remove(neighbor);
            }
        }
    }

    /**
     * Returns all outgoing transfers from an account.
     */
    public synchronized List<TransferEdge> getOutgoingTransfers(String accountNumber) {
        return new ArrayList<>(adjacencyList.getOrDefault(accountNumber, Collections.emptyList()));
    }

    /**
     * Returns the total count of accounts registered in the transfer network.
     */
    public synchronized int getAccountCount() {
        return adjacencyList.size();
    }

    /**
     * Returns total directed edges (transfers) recorded.
     */
    public synchronized int getTotalTransfersCount() {
        int count = 0;
        for (List<TransferEdge> edges : adjacencyList.values()) {
            count += edges.size();
        }
        return count;
    }
}
