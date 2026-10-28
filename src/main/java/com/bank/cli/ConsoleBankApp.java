package com.bank.cli;

import com.bank.domain.*;
import com.bank.exception.BankException;
import com.bank.exception.ValidationException;
import com.bank.service.BankService;
import com.bank.service.BankServiceImpl;
import com.bank.util.AnsiColor;
import com.bank.util.TablePrinter;
import com.bank.util.Validation;

import java.util.List;
import java.util.Optional;
import java.util.Scanner;

/**
 * Interactive Console-based User Interface for the Banking System.
 * Features ASCII banners, formatted tabular reports, resilient input handling,
 * and live DSA diagnostic annotations.
 * Includes interactive validation loops with clear error guidance.
 */
public class ConsoleBankApp {

    private final BankService bankService;
    private final Scanner scanner;

    public ConsoleBankApp(BankService bankService, Scanner scanner) {
        this.bankService = bankService;
        this.scanner = scanner;
    }

    public static void main(String[] args) {
        BankService service = new BankServiceImpl();
        Scanner scanner = new Scanner(System.in);
        ConsoleBankApp app = new ConsoleBankApp(service, scanner);
        app.run();
    }

    public void run() {
        printBanner();
        boolean running = true;

        while (running) {
            printMainMenu();
            String choice = prompt("Enter menu choice [0-12]: ");

            try {
                switch (choice) {
                    case "1"  -> handleOpenAccount();
                    case "2"  -> handleViewAccountStatement();
                    case "3"  -> handleDeposit();
                    case "4"  -> handleWithdraw();
                    case "5"  -> handleTransfer();
                    case "6"  -> handleUndo();
                    case "7"  -> handleTrieCustomerSearch();
                    case "8"  -> handleBstBalanceRangeSearch();
                    case "9"  -> handlePriorityQueueDesk();
                    case "10" -> handleFraudGraphAnalysis();
                    case "11" -> handleCacheDiagnostics();
                    case "12" -> handleLoadDemoData();
                    case "0"  -> {
                        System.out.println(AnsiColor.brightCyan("\nThank you for using the DSA Banking System. Goodbye!"));
                        running = false;
                    }
                    default -> System.out.println(AnsiColor.red("Invalid option. Please enter a number between 0 and 12."));
                }
            } catch (BankException e) {
                System.out.println(AnsiColor.red("\n[BANK ERROR] " + e.getMessage()));
            } catch (Exception e) {
                System.out.println(AnsiColor.red("\n[SYSTEM ERROR] Unexpected error: " + e.getMessage()));
            }

            if (running) {
                promptEnterToContinue();
            }
        }
    }

    // =========================================================================
    // MENU ACTIONS WITH INTERACTIVE VALIDATION
    // =========================================================================

    private void handleOpenAccount() {
        System.out.println(AnsiColor.bold("\n--- OPEN NEW BANK ACCOUNT ---"));

        String name = promptValidatedName();
        String email = promptValidatedEmail();
        String phone = promptValidatedPhone();

        System.out.println("Available Account Types: [1] SAVINGS (Min $100)  [2] CHECKING (Min $0)  [3] FIXED_DEPOSIT (Min $500)");
        String typeChoice = prompt("Select Type (1/2/3, default 1): ");
        String accountType = switch (typeChoice) {
            case "2" -> "CHECKING";
            case "3" -> "FIXED_DEPOSIT";
            default -> "SAVINGS";
        };

        AccountType parsedType = Validation.validateAndParseAccountType(accountType);

        System.out.println("Customer Tiers: [1] REGULAR  [2] SILVER  [3] GOLD  [4] PLATINUM");
        String tierChoice = prompt("Select Tier (1/2/3/4, default 1): ");
        CustomerTier tier = switch (tierChoice) {
            case "2" -> CustomerTier.SILVER;
            case "3" -> CustomerTier.GOLD;
            case "4" -> CustomerTier.PLATINUM;
            default -> CustomerTier.REGULAR;
        };

        double minBalance = parsedType.getMinimumBalance();
        double initialDeposit = promptNonNegativeDouble(String.format("Initial Deposit Amount (Min $%.2f for %s): $",
                minBalance, parsedType.getDisplayName()), minBalance);

        String accountNumber = bankService.openAccount(name, email, phone, accountType, initialDeposit, tier);

        System.out.println(AnsiColor.green("\n✔ Account successfully created!"));
        System.out.println(AnsiColor.bold("  Account Number: ") + AnsiColor.brightCyan(accountNumber));
        System.out.println(AnsiColor.dim("  [DSA: Indexed in Primary HashMap, LRU Cache, Balance BST, and Customer Name Trie]"));
    }

    private void handleViewAccountStatement() {
        System.out.println(AnsiColor.bold("\n--- ACCOUNT STATEMENT & AUDIT ---"));
        String accNum = promptValidatedAccountNumber("Enter Account Number: ");

        Account account = bankService.getAccount(accNum);

        TablePrinter summaryTable = new TablePrinter("Property", "Value");
        summaryTable.setTitle("Account Overview - " + account.getAccountNumber());
        summaryTable.addRow("Account Number", account.getAccountNumber());
        summaryTable.addRow("Account Holder", account.getCustomerName());
        summaryTable.addRow("Customer ID", account.getCustomerId());
        summaryTable.addRow("Account Type", account.getAccountType().getDisplayName());
        summaryTable.addRow("Current Balance", String.format("$%.2f", account.getBalance()));
        summaryTable.addRow("Status", account.isActive() ? AnsiColor.green("ACTIVE") : AnsiColor.red("CLOSED/FROZEN"));
        summaryTable.print();

        List<Transaction> statement = bankService.getAccountStatement(accNum);
        TablePrinter txTable = new TablePrinter("Tx ID", "Timestamp", "Type", "Amount", "Balance After", "Description");
        txTable.setTitle("Transaction Statement");
        txTable.setRightAlign(3, true);
        txTable.setRightAlign(4, true);

        for (Transaction tx : statement) {
            String color = switch (tx.getType()) {
                case DEPOSIT, TRANSFER_IN -> AnsiColor.green(String.format("+ $%.2f", tx.getAmount()));
                case WITHDRAWAL, TRANSFER_OUT -> AnsiColor.red(String.format("- $%.2f", tx.getAmount()));
                case REVERSAL -> AnsiColor.yellow(String.format("~ $%.2f", tx.getAmount()));
            };

            txTable.addRow(
                    tx.getTransactionId(),
                    tx.getFormattedTimestamp(),
                    tx.getType().name(),
                    color,
                    String.format("$%.2f", tx.getBalanceAfter()),
                    tx.getNote()
            );
        }
        txTable.print();
        System.out.println(AnsiColor.dim("[DSA: Retrieved from Transaction List & Account L1 LRU Cache]"));
    }

    private void handleDeposit() {
        System.out.println(AnsiColor.bold("\n--- DEPOSIT FUNDS ---"));
        String accNum = promptValidatedAccountNumber("Account Number: ");
        double amount = promptPositiveDouble("Deposit Amount ($): ");
        String note = prompt("Note / Reference (optional): ");

        Transaction tx = bankService.deposit(accNum, amount, note);
        System.out.println(AnsiColor.green(String.format("✔ Deposited $%.2f into %s. New Balance: $%.2f",
                amount, accNum, tx.getBalanceAfter())));
        System.out.println(AnsiColor.dim("  [DSA: Recorded in Audit Stack for 1-click Undo]"));
    }

    private void handleWithdraw() {
        System.out.println(AnsiColor.bold("\n--- WITHDRAW FUNDS ---"));
        String accNum = promptValidatedAccountNumber("Account Number: ");
        double amount = promptPositiveDouble("Withdrawal Amount ($): ");
        String note = prompt("Note / Reference (optional): ");

        Transaction tx = bankService.withdraw(accNum, amount, note);
        System.out.println(AnsiColor.green(String.format("✔ Withdrew $%.2f from %s. New Balance: $%.2f",
                amount, accNum, tx.getBalanceAfter())));
        System.out.println(AnsiColor.dim("  [DSA: Minimum balance constraint validated; updated in Balance BST]"));
    }

    private void handleTransfer() {
        System.out.println(AnsiColor.bold("\n--- TRANSFER FUNDS ---"));
        String fromAcc = promptValidatedAccountNumber("Source Account (From): ");
        String toAcc = promptValidatedAccountNumber("Destination Account (To): ");

        while (fromAcc.equalsIgnoreCase(toAcc)) {
            System.out.println(AnsiColor.red("Source and destination accounts cannot be identical. Please enter a different destination."));
            toAcc = promptValidatedAccountNumber("Destination Account (To): ");
        }

        double amount = promptPositiveDouble("Transfer Amount ($): ");
        String note = prompt("Transfer Note: ");

        List<Transaction> txs = bankService.transfer(fromAcc, toAcc, amount, note);
        System.out.println(AnsiColor.green(String.format("✔ Successfully transferred $%.2f from %s to %s.",
                amount, fromAcc, toAcc)));
        System.out.println(AnsiColor.dim("  [DSA: Graph Edge recorded in MoneyFlowGraph for AML tracing & pushed to Undo Stack]"));
    }

    private void handleUndo() {
        System.out.println(AnsiColor.bold("\n--- UNDO LAST FINANCIAL OPERATION (LIFO STACK) ---"));
        boolean success = bankService.undoLastOperation();
        if (success) {
            System.out.println(AnsiColor.green("✔ The most recent operation was successfully rolled back and reversed in the ledger!"));
            System.out.println(AnsiColor.dim("  [DSA IN ACTION: Stack.pop() executed Command Pattern reversal in O(1)]"));
        } else {
            System.out.println(AnsiColor.yellow("No reversible transactions found in the undo stack."));
        }
    }

    private void handleTrieCustomerSearch() {
        System.out.println(AnsiColor.bold("\n--- TRIE PREFIX SEARCH (INSTANT AUTOCOMPLETE) ---"));
        System.out.println("Search customer names by character prefix (e.g., 'Al', 'Bo', 'Cat', 'Dav'):");
        String prefix = prompt("Enter Name Prefix: ");

        long startTime = System.nanoTime();
        List<Customer> matches = bankService.searchCustomersByPrefix(prefix);
        long elapsedNanos = System.nanoTime() - startTime;

        TablePrinter table = new TablePrinter("Customer ID", "Name", "Email", "Phone", "Tier");
        table.setTitle(String.format("Trie Autocomplete Matches for '%s' (%d found)", prefix, matches.size()));

        for (Customer c : matches) {
            table.addRow(c.getCustomerId(), c.getName(), c.getEmail(), c.getPhone(), c.getTier().getLabel());
        }
        table.print();

        System.out.println(AnsiColor.cyan(String.format("⚡ [DSA IN ACTION: Prefix Trie lookup executed in %.3f ms (O(P + K) complexity)]",
                elapsedNanos / 1_000_000.0)));
    }

    private void handleBstBalanceRangeSearch() {
        System.out.println(AnsiColor.bold("\n--- BINARY SEARCH TREE RANGE QUERY ---"));
        System.out.println("Query accounts by balance boundaries (e.g. Min: 1000, Max: 10000):");

        double min = promptNonNegativeDouble("Minimum Balance ($): ", 0.0);
        double max;
        while (true) {
            max = promptNonNegativeDouble("Maximum Balance ($): ", 0.0);
            if (max >= min) break;
            System.out.println(AnsiColor.red(String.format("Maximum balance ($%.2f) cannot be less than minimum balance ($%.2f).", max, min)));
        }

        long startTime = System.nanoTime();
        List<Account> matches = bankService.findAccountsByBalanceRange(min, max);
        long elapsedNanos = System.nanoTime() - startTime;

        TablePrinter table = new TablePrinter("Account Number", "Account Holder", "Type", "Balance", "Status");
        table.setTitle(String.format("Accounts with Balance in Range [$%.2f - $%.2f]", min, max));
        table.setRightAlign(3, true);

        for (Account a : matches) {
            table.addRow(
                    a.getAccountNumber(),
                    a.getCustomerName(),
                    a.getAccountType().name(),
                    String.format("$%.2f", a.getBalance()),
                    a.isActive() ? "ACTIVE" : "FROZEN"
            );
        }
        table.print();

        System.out.println(AnsiColor.cyan(String.format("⚡ [DSA IN ACTION: BST range search executed in %.3f ms (O(log N + K) complexity)]",
                elapsedNanos / 1_000_000.0)));
    }

    private void handlePriorityQueueDesk() {
        System.out.println(AnsiColor.bold("\n--- VIP PRIORITY CUSTOMER DESK (BINARY MAX-HEAP) ---"));
        System.out.println("1) Submit New Support Ticket");
        System.out.println("2) Process Next Highest Priority Ticket (extractMax)");
        System.out.println("3) View Current Priority Queue Order");
        String subChoice = prompt("Select Option (1/2/3): ");

        switch (subChoice) {
            case "1" -> {
                String custId = prompt("Customer ID (e.g., CUST-1001): ");
                String query = prompt("Issue / Request Details: ");
                ServiceRequest req = bankService.submitServiceRequest(custId, query);
                System.out.println(AnsiColor.green("✔ Ticket submitted: " + req.getRequestId()));
                System.out.println(AnsiColor.dim("  [DSA: Sifted up into Binary Max-Heap in O(log N)]"));
            }
            case "2" -> {
                Optional<ServiceRequest> reqOpt = bankService.processNextPriorityRequest();
                if (reqOpt.isPresent()) {
                    ServiceRequest req = reqOpt.get();
                    System.out.println(AnsiColor.brightCyan("\n>>> DISPATCHING HIGHEST PRIORITY TICKET <<<"));
                    System.out.println("  Ticket ID:    " + req.getRequestId());
                    System.out.println("  Customer:     " + req.getCustomerName() + " (" + req.getCustomerId() + ")");
                    System.out.println("  Priority:     " + AnsiColor.bold(req.getTier().getLabel()));
                    System.out.println("  Submitted At: " + req.getFormattedTimestamp());
                    System.out.println("  Details:      " + req.getQueryDetails());
                    System.out.println(AnsiColor.dim("  [DSA IN ACTION: Max-Heap root extracted in O(log N) with siftDown]"));
                } else {
                    System.out.println(AnsiColor.yellow("No pending service requests in queue."));
                }
            }
            case "3" -> {
                List<ServiceRequest> queue = bankService.listPendingServiceRequests();
                TablePrinter table = new TablePrinter("Rank", "Ticket ID", "Tier / Priority", "Customer", "Query Details");
                table.setTitle("Priority Support Queue (Sorted by Heap Priority)");

                int rank = 1;
                for (ServiceRequest req : queue) {
                    table.addRow(String.valueOf(rank++), req.getRequestId(), req.getTier().getLabel(),
                            req.getCustomerName(), req.getQueryDetails());
                }
                table.print();
            }
            default -> System.out.println(AnsiColor.red("Invalid option."));
        }
    }

    private void handleFraudGraphAnalysis() {
        System.out.println(AnsiColor.bold("\n--- FORENSIC GRAPH ANALYSIS & ANTI-MONEY LAUNDERING (AML) ---"));
        System.out.println("1) Trace Shortest Transfer Path Between Two Accounts (BFS)");
        System.out.println("2) Detect Circular Money Laundering Loops (DFS Cycle Detection)");
        String subChoice = prompt("Select Option (1/2): ");

        switch (subChoice) {
            case "1" -> {
                String from = promptValidatedAccountNumber("Source Account: ");
                String to = promptValidatedAccountNumber("Target Account: ");
                List<String> path = bankService.traceMoneyTrail(from, to);

                if (path.isEmpty()) {
                    System.out.println(AnsiColor.yellow(String.format("No transfer route connects %s and %s.", from, to)));
                } else {
                    System.out.println(AnsiColor.green("\n✔ Shortest Transfer Path Found (BFS):"));
                    System.out.println("  " + AnsiColor.bold(String.join(" ──► ", path)));
                    System.out.println("  Total transfer hops: " + (path.size() - 1));
                    System.out.println(AnsiColor.dim("  [DSA IN ACTION: Breadth-First Search discovered path in O(V + E) time]"));
                }
            }
            case "2" -> {
                String acc = promptValidatedAccountNumber("Account to Audit for Circular Flows: ");
                List<List<String>> cycles = bankService.detectCircularFlows(acc);

                if (cycles.isEmpty()) {
                    System.out.println(AnsiColor.green("✔ No circular money flows detected for " + acc + "."));
                } else {
                    System.out.println(AnsiColor.red(String.format("\n⚠ WARNING: %d CIRCULAR FLOW CYCLE(S) DETECTED!", cycles.size())));
                    for (int i = 0; i < cycles.size(); i++) {
                        System.out.println(AnsiColor.bold(String.format("  Cycle #%d: ", i + 1)) +
                                AnsiColor.brightRed(String.join(" ──► ", cycles.get(i))));
                    }
                    System.out.println(AnsiColor.dim("  [DSA IN ACTION: Depth-First Search with Backtracking detected circular ring in O(V + E)]"));
                }
            }
            default -> System.out.println(AnsiColor.red("Invalid option."));
        }
    }

    private void handleCacheDiagnostics() {
        System.out.println(AnsiColor.bold("\n--- SYSTEM DIAGNOSTICS & LRU CACHE TELEMETRY ---"));
        System.out.println(AnsiColor.brightCyan(bankService.getCacheDiagnostics()));

        List<Account> accounts = bankService.listAllAccounts();
        TablePrinter table = new TablePrinter("Account Number", "Holder", "Type", "Balance", "Status");
        table.setTitle(String.format("System Accounts Inventory (%d registered)", accounts.size()));
        table.setRightAlign(3, true);

        for (Account a : accounts) {
            table.addRow(
                    a.getAccountNumber(),
                    a.getCustomerName(),
                    a.getAccountType().name(),
                    String.format("$%.2f", a.getBalance()),
                    a.isActive() ? AnsiColor.green("ACTIVE") : AnsiColor.red("CLOSED")
            );
        }
        table.print();
    }

    private void handleLoadDemoData() {
        System.out.println(AnsiColor.bold("\n--- SEED REALISTIC DEMO DATASET ---"));
        bankService.seedDemoData();
        System.out.println(AnsiColor.green("✔ Successfully loaded 6 customers, 6 accounts, multiple transfers, and VIP tickets!"));
        System.out.println("  * Accounts generated: AC-100001 through AC-100006");
        System.out.println("  * Graph cycle created: AC-100001 -> AC-100002 -> AC-100003 -> AC-100001 (Test with option 10!)");
        System.out.println("  * Customers available for prefix search: 'Alice', 'Alexander', 'Bob', 'Catherine', 'David', 'Emma'");
    }

    // =========================================================================
    // VALIDATING UI PROMPTS (INTERACTIVE LOOPS)
    // =========================================================================

    private String promptValidatedName() {
        while (true) {
            String name = prompt("Customer Full Name: ");
            try {
                Validation.validateName(name);
                return name.trim();
            } catch (ValidationException e) {
                System.out.println(AnsiColor.red("  ✖ [NAME VALIDATION ERROR] " + e.getMessage()));
            }
        }
    }

    private String promptValidatedEmail() {
        while (true) {
            String email = prompt("Email Address: ");
            try {
                Validation.validateEmail(email);
                return email.trim();
            } catch (ValidationException e) {
                System.out.println(AnsiColor.red("  ✖ [EMAIL VALIDATION ERROR] " + e.getMessage()));
            }
        }
    }

    private String promptValidatedPhone() {
        while (true) {
            String phone = prompt("Phone Number: ");
            try {
                Validation.validatePhone(phone);
                return phone.trim();
            } catch (ValidationException e) {
                System.out.println(AnsiColor.red("  ✖ [PHONE VALIDATION ERROR] " + e.getMessage()));
            }
        }
    }

    private String promptValidatedAccountNumber(String message) {
        while (true) {
            String acc = prompt(message);
            try {
                Validation.validateAccountNumber(acc);
                return acc.trim().toUpperCase();
            } catch (ValidationException e) {
                System.out.println(AnsiColor.red("  ✖ [ACCOUNT NUMBER ERROR] " + e.getMessage()));
            }
        }
    }

    private double promptPositiveAmount(String message) {
        while (true) {
            double amt = promptDouble(message);
            try {
                Validation.validatePositiveAmount(amt, "Amount");
                return amt;
            } catch (ValidationException e) {
                System.out.println(AnsiColor.red("  ✖ [AMOUNT ERROR] " + e.getMessage()));
            }
        }
    }

    private double promptPositiveDouble(String message) {
        return promptPositiveAmount(message);
    }

    private double promptNonNegativeDouble(String message, double minimumRequired) {
        while (true) {
            double val = promptDouble(message);
            if (val < 0.0) {
                System.out.println(AnsiColor.red("  ✖ Amount cannot be negative. Please enter a valid number."));
                continue;
            }
            if (val < minimumRequired) {
                System.out.println(AnsiColor.red(String.format("  ✖ Amount ($%.2f) is below the minimum required of $%.2f.", val, minimumRequired)));
                continue;
            }
            return val;
        }
    }

    private String prompt(String message) {
        System.out.print(AnsiColor.bold(message));
        return scanner.nextLine().trim();
    }

    private double promptDouble(String message) {
        while (true) {
            System.out.print(AnsiColor.bold(message));
            String line = scanner.nextLine().trim();
            if (line.isEmpty()) {
                return 0.0;
            }
            try {
                return Double.parseDouble(line);
            } catch (NumberFormatException e) {
                System.out.println(AnsiColor.red("  ✖ Please enter a valid numeric amount (e.g., 250.50)."));
            }
        }
    }

    private void promptEnterToContinue() {
        System.out.print(AnsiColor.dim("\n[Press Enter to continue...]"));
        scanner.nextLine();
    }
}
