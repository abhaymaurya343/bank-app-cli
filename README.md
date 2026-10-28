# 🏦 Enterprise CLI Banking System (DSA-Powered)

An enterprise-grade, high-performance Command Line Banking backend featuring dual execution engines in pure **Core Java** and native **Python**. This project is directly inspired by and builds upon the foundational banking domain from [Faisal Memon's 100-Day Java Backend Masterclass](https://github.com/buildwithfaisal/100-day-java-backend-masterclass/tree/main/java/bank-app), evolving it into an algorithmically optimized, production-patterned architecture powered by real-world **Data Structures and Algorithms (DSA)** and rigorous **Enterprise Validation & Error Handling**.

---

## 📑 Table of Contents
1. [Project Overview & Motivation](#-project-overview--motivation)
2. [Architectural Overview](#-architectural-overview)
3. [Deep-Dive: Data Structures & Algorithms Applied](#-deep-dive-data-structures--algorithms-applied)
   - [1. Trie (Prefix Tree) — Customer Autocomplete](#1-trie-prefix-tree--customer-autocomplete)
   - [2. LRU Cache (DLL + HashMap) — Hot Account Caching](#2-lru-cache-doubly-linked-list--hashmap--hot-account-caching)
   - [3. Binary Search Tree — Range Queries on Balances](#3-binary-search-tree--range-queries-on-balances)
   - [4. Binary Max-Heap — VIP Customer Service Desk](#4-binary-max-heap--vip-customer-service-desk)
   - [5. Directed Graph (BFS & DFS) — Anti-Money Laundering (AML) Analysis](#5-directed-graph-bfs--dfs--anti-money-laundering-aml-analysis)
   - [6. LIFO Stack (Command Pattern) — Transaction Undo & Rollback](#6-lifo-stack-command-pattern--transaction-undo--rollback)
4. [Enterprise Validation & Granular Error Handling](#-enterprise-validation--granular-error-handling)
   - [Granular Email Validation & Feedback](#granular-email-validation--feedback)
   - [Phone, Name, and Account Format Verification](#phone-name-and-account-format-verification)
   - [Interactive In-CLI Recovery Loops](#interactive-in-cli-recovery-loops)
5. [Complexity Summary Table](#-complexity-summary-table)
6. [Comparison with Reference Bank App](#-comparison-with-reference-bank-app)
7. [Project Structure](#-project-structure)
8. [Getting Started & Running the Project](#-getting-started--running-the-project)
   - [Method 1: Instant Python CLI Engine (Zero-Setup)](#method-1-instant-python-cli-engine-zero-setup)
   - [Method 2: One-Click Desktop Batch Launcher](#method-2-one-click-desktop-batch-launcher)
   - [Method 3: Native Java Compilation (`javac` / `java`)](#method-3-native-java-compilation-javac--java)
   - [Method 4: Using Apache Maven](#method-4-using-apache-maven)
9. [Running the Automated Test Suite](#-running-the-automated-test-suite)
10. [CLI Feature Walkthrough & Menu Guide](#-cli-feature-walkthrough--menu-guide)
11. [Key Design Patterns Utilized](#-key-design-patterns-utilized)

---

## 🌟 Project Overview & Motivation

In traditional beginner bank apps, data management is typically limited to in-memory `ArrayList`s or basic `HashMap`s with linear scans (`O(N)`). While suitable for introductory tutorials, real-world financial core banking systems process millions of accounts and require:

- Sub-millisecond autocomplete for customer search without freezing the console.
- In-memory L1 caching to serve high-frequency ("hot") accounts without hitting disks.
- Fast range queries for wealth management and audit compliance.
- Strict priority queue scheduling for high-net-worth (VIP) clients.
- Forensic graph analysis to detect circular money laundering rings and fund trails.
- Atomic reversible audit trails allowing tellers to undo mistaken inputs.
- Resilient input validation preventing corrupted or malformed records from reaching the ledger.

This project implements all these capabilities natively with **zero third-party runtime dependencies**.

---

## 🏛 Architectural Overview

The application follows the **Layered Clean Architecture** pattern:

```mermaid
flowchart TD
    CLI["CLI Presentation Layer\n(ConsoleBankApp, TablePrinter, AnsiColor)"]
    Service["Service Orchestration Layer\n(BankService, BankServiceImpl)"]
    Validation["Validation & Security Layer\n(Validation.java, RFC 5322 & E.164 Engine)"]
    Repo["Repository Layer\n(AccountRepository, CustomerRepository, TransactionRepository)"]
    DSA["Custom DSA Layer\n(Trie, LRUCache, BST, MaxHeap, MoneyFlowGraph, AuditStack)"]
    Domain["Domain Model Layer\n(Account, Customer, Transaction, ServiceRequest)"]

    CLI --> Validation
    CLI --> Service
    Service --> Validation
    Service --> Repo
    Service --> DSA
    Repo --> DSA
    Repo --> Domain
    Service --> Domain
```

### Layer Responsibilities
* **`com.bank.cli`**: Formats interactive CLI menus, ASCII banners, colored status outputs, auto-sizing tabular statements, and validating input prompt loops.
* **`com.bank.service`**: Encapsulates business logic, balance validation, state transitions, and coordination between repositories and DSA modules.
* **`com.bank.repository`**: Provides data access abstractions combining primary hash storage with secondary DSA indexes.
* **`com.bank.dsa`**: Clean, self-contained, custom-built data structures designed specifically for banking problem domains.
* **`com.bank.domain`**: Rich domain entities (`Account`, `Customer`, `Transaction`, `ServiceRequest`, and domain enums).
* **`com.bank.util`**: Enterprise validation helpers, ANSI color formatters, and table alignment renderers.
* **`com.bank.exception`**: Domain-specific unchecked exceptions (`ValidationException`, `InsufficientFundsException`, `AccountNotFoundException`, etc.).

---

## 🔬 Deep-Dive: Data Structures & Algorithms Applied

### 1. Trie (Prefix Tree) — Customer Autocomplete
* **Package**: `com.bank.dsa.Trie<V>`
* **The Banking Problem**:
  When a bank employee types "Al" into the search bar, the system must immediately suggest all matching customers ("Alice Smith", "Alexander Wright"). Scanning an `ArrayList` of 1,000,000 customer strings with `.startsWith()` requires $O(N \cdot L)$ string comparisons on every keystroke, degrading latency.
* **How It Works**:
  A tree where each node represents a character. Words sharing common prefixes share the same initial branch. In our implementation, both the full name and space-separated tokens (first name and last name) are indexed. Each node maintains a collection of payload references.
* **Complexity**:
  - Search Prefix: $O(P + K)$ where $P = \text{length of prefix}$ and $K = \text{number of matched records}$.
  - Independent of total customer count $N$.

```
           [root]
          /      \
        'a'      'b'
         |        |
        'l'      'o'
       /   \      |
     'i'   'e'   'b' -> [Bob]
     /       \
   'c'       'x' -> [Alexander]
   /
 'e' -> [Alice]
```

---

### 2. LRU Cache (Doubly Linked List + HashMap) — Hot Account Caching
* **Package**: `com.bank.dsa.LRUCache<K, V>`
* **The Banking Problem**:
  The 80/20 Pareto principle applies to banking: ~20% of accounts generate ~80% of daily transactions (salary deposits, merchant billings, ATM checks). Fetching records repeatedly from secondary storage is inefficient. An in-memory cache must retain the most active accounts and evict idle ones.
* **How It Works**:
  - **HashMap**: Provides $O(1)$ lookup mapping `accountNumber -> Node`.
  - **Doubly Linked List**: Maintains access recency.
    - `Head`: Most Recently Used (MRU) account.
    - `Tail`: Least Recently Used (LRU) account.
  - When an account is accessed (`get`), it is unlinked and spliced at `Head` in $O(1)$.
  - When cache capacity is exceeded upon `put`, the node right before `Tail` is evicted in $O(1)$.
* **Complexity**:
  - `get(key)`: $O(1)$
  - `put(key, value)`: $O(1)$
  - `remove(key)`: $O(1)$

---

### 3. Binary Search Tree — Range Queries on Balances
* **Package**: `com.bank.dsa.BinarySearchTree<K, V>`
* **The Banking Problem**:
  Financial compliance and wealth management frequently run range audits: *"Find all accounts with balances between \$5,000 and \$25,000"*. Filtering an unsorted list is $O(N)$.
* **How It Works**:
  - Accounts are ordered by their balance.
  - To handle duplicate balances (e.g. multiple accounts with \$1,000.00), each BST node holds a bucket `List<V>`.
  - **Range Pruning Algorithm**: During recursion, if `current.key < min`, the entire left subtree is discarded without traversal. If `current.key > max`, the entire right subtree is discarded.
* **Complexity**:
  - Average Range Query: $O(\log N + K)$, where $K$ is the number of qualifying accounts.
  - In-order traversal: $O(N)$ produces a fully sorted balance list.

---

### 4. Binary Max-Heap — VIP Customer Service Desk
* **Package**: `com.bank.dsa.MaxHeap<T>`
* **The Banking Problem**:
  In high-traffic customer support and relationship banking, requests cannot be served strictly First-Come, First-Served (FIFO). A Platinum VIP client facing a wire transfer issue must be routed to a senior relationship manager ahead of general inquiries.
* **How It Works**:
  - Complete binary tree implemented in a dynamic contiguous array.
  - Max-Heap property: Parent priority score $\ge$ children priority score.
  - For node at index $i$:
    - Left Child: $2i + 1$
    - Right Child: $2i + 2$
    - Parent: $\lfloor (i - 1) / 2 \rfloor$
  - Priority comparison ranks `PLATINUM(4) > GOLD(3) > SILVER(2) > REGULAR(1)`, using submission timestamp as a deterministic tie-breaker.
* **Complexity**:
  - Peek highest priority: $O(1)$
  - Insert new request (`siftUp`): $O(\log N)$
  - Dispatch highest priority request (`extractMax` with `siftDown`): $O(\log N)$

---

### 5. Directed Graph (BFS & DFS) — Anti-Money Laundering (AML) Analysis
* **Package**: `com.bank.dsa.MoneyFlowGraph`
* **The Banking Problem**:
  - **Money Trail Tracing**: When tracking stolen funds, investigators need the shortest chain of transfer hops between Account A and Account Z.
  - **AML Circular Ring Detection**: A common money laundering pattern ("smurfing" or "layering") bounces funds through intermediate dummy/mule accounts back to the perpetrator (`A -> B -> C -> A`) to obscure origins.
* **How It Works**:
  - **Graph Representation**: Adjacency list `Map<String, List<TransferEdge>>`.
  - **Shortest Path (BFS)**: Uses a queue and predecessor map to explore outward layer-by-layer, guaranteeing the shortest hop chain in $O(V + E)$.
  - **Cycle Detection (DFS with Backtracking)**: Explores transfer chains from a suspect account. If a directed path encounters the originating account again, an AML cycle alert is triggered.
* **Complexity**:
  - BFS Path Search: $O(V + E)$
  - DFS Cycle Detection: $O(V + E)$ where $V = \text{accounts}$, $E = \text{transfers}$.

---

### 6. LIFO Stack (Command Pattern) — Transaction Undo & Rollback
* **Package**: `com.bank.dsa.TransactionAuditStack`
* **The Banking Problem**:
  Tellers make typographical errors (e.g. entering \$1,000 instead of \$100, or selecting the wrong recipient). The system must allow immediate one-click rollbacks of recent operations while maintaining audit log integrity.
* **How It Works**:
  - Every mutation registers a `ReversibleAction` lambda onto a Last-In, First-Out (LIFO) stack.
  - Invoking Undo pops the most recent action, restores balances atomically, and writes an official `REVERSAL` transaction to the ledger.
* **Complexity**:
  - Push reversible operation: $O(1)$
  - Pop and execute rollback: $O(1)$

---

## 🛡️ Enterprise Validation & Granular Error Handling

Both the Java and Python implementations feature an industrial-grade input validation framework (`Validation.java` and `run.py`).

### Granular Email Validation & Feedback
Rather than generic rejection, the validation engine pinpoints the exact structural flaw:

| Malformed Email Input | Pinpointed Error Message |
| :--- | :--- |
| `""` or `"   "` | `✖ Email address cannot be empty or blank.` |
| `"user name@domain.com"` | `✖ Invalid email: email address cannot contain spaces.` |
| `"userdomain.com"` | `✖ Invalid email format: missing '@' symbol (e.g. user@domain.com).` |
| `"user@@domain.com"` | `✖ Invalid email format: contains multiple '@' symbols.` |
| `"@domain.com"` | `✖ Invalid email format: missing username/local part before '@'.` |
| `"user@"` | `✖ Invalid email format: missing domain name after '@' (e.g. domain.com).` |
| `"user@domain"` | `✖ Invalid email domain: missing dot extension (e.g. .com, .org).` |
| `"user@domain.c"` | `✖ Invalid email domain: TLD extension '.c' is too short (minimum 2 characters).` |
| `"user@example.com"` | `✔ Validated & accepted via RFC 5322 compliance.` |

### Phone, Name, and Account Format Verification
* **Customer Name**: Enforces lengths between 2 and 100 characters. Rejects digits and punctuation (allowing only letters, hyphens, and apostrophes).
* **Phone Numbers**: Complies with **ITU-T E.164** standards: requires 7 to 15 digits; allows international prefix `+` and standard separators (`-`, ` `).
* **Account Numbers**: Strictly validates the format `AC-XXXXXX` (e.g. `AC-100001`). Rejects malformed strings before repository execution.
* **Monetary Values**: Prevents non-numeric input, `NaN`, `Infinity`, negative amounts, zero amounts for deposits/withdrawals, and enforces a \$100,000,000.00 single-transaction ceiling.
* **Interval Constraints**: Enforces `minimum_balance <= maximum_balance` for BST range queries.

### Interactive In-CLI Recovery Loops
In the interactive console, if a user makes a typo in their email address or deposit amount, the CLI **does not crash or discard the form**. Instead, it presents the specific validation message in red and immediately prompts for that field again, creating a smooth user experience.

---

## 📊 Complexity Summary Table

| Operation / Feature | Underlying DSA | Time Complexity | Space Complexity | Real-World Benefit |
| :--- | :--- | :--- | :--- | :--- |
| **Customer Name Autocomplete** | Trie (Prefix Tree) | $O(P + K)$ | $O(\Sigma \cdot L \cdot N)$ | Instant search across millions of customer names |
| **Hot Account L1 Cache** | LRU Cache (DLL + HashMap) | $O(1)$ | $O(\text{Capacity})$ | Zero-delay lookups for frequent transactions |
| **Balance Range Inquiries** | Binary Search Tree | $O(\log N + K)$ | $O(N)$ | Prunes subtrees; fast audits and wealth tier queries |
| **VIP Support Triage** | Binary Max-Heap | $O(\log N)$ insert, $O(1)$ peek | $O(N)$ | Guarantees VIP clients are prioritized automatically |
| **Money Trail Search** | Directed Graph BFS | $O(V + E)$ | $O(V)$ | Forensic tracking of fund hops between accounts |
| **AML Circular Cycle Alert** | Directed Graph DFS | $O(V + E)$ | $O(V + E)$ | Unmasks money laundering layering rings |
| **Transaction Undo** | ArrayDeque Stack | $O(1)$ | $O(U)$ | Operational error recovery with audit integrity |

---

## ⚖ Comparison with Reference Bank App

| Feature | Reference Repo (`buildwithfaisal/bank-app`) | This Enterprise CLI DSA Project |
| :--- | :--- | :--- |
| **User Interface** | Basic scanner loop | Rich interactive CLI with ANSI colors & formatted ASCII tables |
| **Input Validation** | Basic check on deposit > 0 | Granular RFC 5322 email, E.164 phone, name, and bounds checking |
| **Error Recovery** | Uncaught exceptions abort flow | Interactive prompt loops re-request invalid fields seamlessly |
| **Execution Engines** | Java only | Dual-engine: Native Core Java + Zero-Setup Python CLI |
| **Customer Search** | Linear Stream filter ($O(N)$) | Custom Trie with tokenized prefix search ($O(P + K)$) |
| **Account Storage** | Raw `HashMap` without caching | Tiered L1 LRU Cache ($O(1)$) + Primary Hash Index |
| **Balance Inquiries** | Linear scan | Binary Search Tree with branch-pruning range queries |
| **Priority Handling** | None | Binary Max-Heap priority queue based on customer tier |
| **AML & Fraud Detection**| None | Directed Money Flow Graph with BFS trail & DFS cycle detection |
| **Operational Rollback**| None | Command Pattern undo stack with `REVERSAL` audit trail |
| **Demo Dataset** | None (manual input required) | 1-Click synthetic seeder with accounts, transfers, and cycles |
| **Automated Tests** | None | Comprehensive test suite covering DSA & validation edge cases |

---

## 📁 Project Structure

```
bank-app-cli/
├── pom.xml                                   # Maven build configuration (Java 17+)
├── run.bat                                   # Windows launcher (auto-detects Java or Python)
├── run.py                                    # Instant zero-dependency native Python CLI engine
├── run.sh                                    # Unix/macOS one-click compile & launch script
├── README.md                                 # Comprehensive in-depth documentation
└── src/
    ├── main/java/com/bank/
    │   ├── cli/
    │   │   └── ConsoleBankApp.java           # Interactive CLI menu & terminal driver
    │   ├── domain/
    │   │   ├── Account.java                  # Financial account entity
    │   │   ├── AccountType.java              # SAVINGS, CHECKING, FIXED_DEPOSIT enum
    │   │   ├── Customer.java                 # Customer profile entity
    │   │   ├── CustomerTier.java             # REGULAR, SILVER, GOLD, PLATINUM enum
    │   │   ├── ServiceRequest.java           # Priority desk support ticket
    │   │   ├── Transaction.java              # Ledger audit entry
    │   │   └── TransactionType.java          # DEPOSIT, WITHDRAWAL, TRANSFER, REVERSAL
    │   ├── dsa/
    │   │   ├── BinarySearchTree.java         # BST supporting duplicate keys & range pruning
    │   │   ├── LRUCache.java                 # Custom Doubly Linked List + HashMap cache
    │   │   ├── MaxHeap.java                  # Array-backed Binary Max-Heap
    │   │   ├── MoneyFlowGraph.java           # Directed Graph with BFS shortest path & DFS cycles
    │   │   ├── TransactionAuditStack.java    # LIFO stack for Command Pattern rollbacks
    │   │   └── Trie.java                     # Prefix Tree for instant name autocomplete
    │   ├── exception/
    │   │   ├── AccountFrozenException.java
    │   │   ├── AccountNotFoundException.java
    │   │   ├── BankException.java            # Base unchecked banking exception
    │   │   ├── DuplicateAccountException.java
    │   │   ├── InsufficientFundsException.java
    │   │   └── ValidationException.java
    │   ├── repository/
    │   │   ├── AccountRepository.java        # Coordinates HashMap, LRU Cache & BST index
    │   │   ├── CustomerRepository.java       # Coordinates HashMap & Prefix Trie
    │   │   └── TransactionRepository.java    # Coordinates Ledger, Graph & Undo Stack
    │   ├── service/
    │   │   ├── BankService.java              # Service layer business interface
    │   │   └── BankServiceImpl.java          # Core implementation of banking operations
    │   └── util/
    │       ├── AnsiColor.java                # Terminal color styling & escape sequences
    │       ├── TablePrinter.java             # ASCII auto-sizing table renderer
    │       └── Validation.java               # Granular email, phone, name, and bounds engine
    └── test/java/com/bank/
        └── AppTestSuite.java                 # Zero-dependency test runner with 25+ assertions
```

---

## 🚀 Getting Started & Running the Project

### Method 1: Instant Python CLI Engine (Zero-Setup)
If you have Python installed, you can launch the complete banking system immediately with zero setup:
```cmd
cd "C:\Users\Abhay Maurya\.gemini\antigravity\scratch\bank-app-cli"
python run.py
```

### Method 2: One-Click Desktop Batch Launcher
On Windows, double-click the included `run.bat` (or the desktop launcher). It automatically detects if a Java Development Kit (JDK) is present. If found, it compiles and runs the Java engine; otherwise, it seamlessly boots the Python engine!

### Method 3: Native Java Compilation (`javac` / `java`)
To compile and run natively using OpenJDK 17+:
```powershell
cd "C:\Users\Abhay Maurya\.gemini\antigravity\scratch\bank-app-cli"

# Compile all source files
Get-ChildItem -Recurse -Filter *.java src\main\java | Resolve-Path | ForEach-Object { $_.Path } | Out-File -Encoding ascii sources.txt
javac -d out @sources.txt
Remove-Item sources.txt

# Run the application
java -cp out com.bank.cli.ConsoleBankApp
```

### Method 4: Using Apache Maven
```bash
mvn clean compile exec:java
```

---

## 🧪 Running the Automated Test Suite

The test suite validates every DSA module, business operation, and granular validation rule:

```powershell
Get-ChildItem -Recurse -Filter *.java src | Resolve-Path | ForEach-Object { $_.Path } | Out-File -Encoding ascii test_sources.txt
javac -d out @test_sources.txt
Remove-Item test_sources.txt
java -cp out com.bank.AppTestSuite
```

### Test Suite Execution Output:
```
=======================================================
        RUNNING DSA & BANK SERVICE TEST SUITE
=======================================================

[0] Granular Validation & Error Handling
  ✔ [PASS] Valid email passes validation
  ✔ [PASS] Email missing '@' throws ValidationException
  ✔ [PASS] Email with spaces throws ValidationException
  ✔ [PASS] Email missing domain throws ValidationException
  ✔ [PASS] Email missing username throws ValidationException
  ✔ [PASS] Email with single character TLD throws ValidationException
  ✔ [PASS] Negative monetary amount throws ValidationException
  ✔ [PASS] Invalid phone number throws ValidationException
  ✔ [PASS] Invalid account number format throws ValidationException
  ✔ [PASS] Invalid balance range (min > max) throws ValidationException

[1] Trie (Prefix Tree)
  ✔ [PASS] Trie prefix search for 'Al' finds Alice and Alexander
  ✔ [PASS] Trie prefix search for token 'Hamilton'
  ✔ [PASS] Trie search for non-existent prefix returns empty

[2] LRU Cache
  ✔ [PASS] LRU Cache get existing items
  ✔ [PASS] LRU Cache evicts least recently accessed item when full
  ✔ [PASS] LRU Cache hit rate tracking

[3] Binary Search Tree (Range Query)
  ✔ [PASS] BST Range Query [400 - 3000] finds middle accounts
  ✔ [PASS] BST in-order traversal yields sorted elements

[4] Binary Max-Heap (Priority Queue)
  ✔ [PASS] Max-Heap extracts Platinum VIP first, then Gold, then Regular

[5] Money Flow Graph (AML & Shortest Path)
  ✔ [PASS] Graph BFS finds shortest path from A to D
  ✔ [PASS] Graph DFS detects circular AML ring (C -> A)

[6] Transaction Audit Stack (Undo)
  ✔ [PASS] Undo executes last command

[7] Bank Service Financial Operations
  ✔ [PASS] Deposit and balance update
  ✔ [PASS] Transfer and balance integrity
  ✔ [PASS] Undo reverses the transfer

=======================================================
TEST RESULTS: 25 Passed | 0 Failed
=======================================================
```

---

## 💻 CLI Feature Walkthrough & Menu Guide

```text
========================== MAIN NAVIGATION MENU ==========================
 [ 1] Open New Account                   [ 7] Search Customers (Trie Autocomplete)
 [ 2] View Account Statement             [ 8] Range Search Balances (BST Query)
 [ 3] Deposit Funds                      [ 9] VIP Priority Service Desk (Max-Heap)
 [ 4] Withdraw Funds                     [10] AML Fraud & Money Trail (Graph BFS/DFS)
 [ 5] Transfer Funds                     [11] Diagnostics & LRU Cache Telemetry
 [ 6] Undo Last Operation (Stack)        [12] 1-Click Load Realistic Demo Dataset
 [ 0] Exit System
==========================================================================
```

### Recommended Test Tour:
1. **Press `12` (Load Realistic Demo Dataset)**: Seeds 6 accounts (`AC-100001` to `AC-100006`), diverse tiers, and a circular AML ring.
2. **Press `7` (Trie Customer Search)**: Type `Al` for instant sub-millisecond autocomplete of "Alice Smith" and "Alexander Wright".
3. **Press `8` (BST Range Query)**: Enter Min: `1000` and Max: `15000` to retrieve qualifying accounts in $O(\log N + K)$ time.
4. **Press `9` (VIP Priority Support Desk)**: Select `2` (`extractMax`) to see the Max-Heap dispatch Platinum VIP consultations first.
5. **Press `10` (AML Fraud & Forensic Graph)**:
   - Select `1` (BFS shortest path) between `AC-100001` and `AC-100006`.
   - Select `2` (DFS cycle detection) on `AC-100001` to reveal the circular ring:
     `AC-100001 ──► AC-100002 ──► AC-100003 ──► AC-100001`.
6. **Press `6` (Undo Operation)**: Test rolling back the most recent transaction via the Command Pattern stack.
7. **Press `11` (System Diagnostics)**: Inspect live L1 LRU Cache metrics: hit counts, misses, and hit rate percentage.

---

## 📐 Key Design Patterns Utilized

1. **Repository Pattern**: Separates persistence operations from core banking business logic.
2. **Command Pattern**: Encapsulates state mutations as reversible objects stored on the `TransactionAuditStack`.
3. **Strategy / Enum Behavior**: `AccountType` and `CustomerTier` encapsulate business rules directly within types.
4. **Sentinel Node Pattern**: `LRUCache` utilizes pseudo `head` and `tail` nodes to eliminate null checks during $O(1)$ pointer manipulations.

---

## 📜 License
This project is open-source educational software distributed under the MIT License. Feel free to use, modify, and expand upon it for your backend masterclass studies, portfolio, and technical interview preparation!
