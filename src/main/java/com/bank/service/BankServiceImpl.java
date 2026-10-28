package com.bank.service;

import com.bank.domain.*;
import com.bank.dsa.MaxHeap;
import com.bank.exception.*;
import com.bank.repository.AccountRepository;
import com.bank.repository.CustomerRepository;
import com.bank.repository.TransactionRepository;
import com.bank.util.Validation;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Production-ready implementation of BankService orchestrating
 * core banking business logic and DSA indexes.
 * Enforces rigorous domain-level validation and error checking across all operations.
 */
public class BankServiceImpl implements BankService {

    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;
    private final TransactionRepository transactionRepository;
    private final MaxHeap<ServiceRequest> priorityDeskQueue;

    private final AtomicInteger accountSequence = new AtomicInteger(100000);
    private final AtomicInteger customerSequence = new AtomicInteger(1000);

    public BankServiceImpl(AccountRepository accountRepository,
                           CustomerRepository customerRepository,
                           TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.customerRepository = customerRepository;
        this.transactionRepository = transactionRepository;
        this.priorityDeskQueue = new MaxHeap<>();
    }

    public BankServiceImpl() {
        this(new AccountRepository(), new CustomerRepository(), new TransactionRepository());
    }

    // =========================================================================
    // ACCOUNT MANAGEMENT
    // =========================================================================

    @Override
    public synchronized String openAccount(String name, String email, String phone,
                                          String accountTypeStr, double initialDeposit,
                                          CustomerTier tier) {
        Validation.validateName(name);
        Validation.validateEmail(email);
        Validation.validatePhone(phone);
        Validation.validateNonNegativeAmount(initialDeposit, "Initial deposit");
        AccountType accountType = Validation.validateAndParseAccountType(accountTypeStr);

        if (initialDeposit < accountType.getMinimumBalance()) {
            throw new ValidationException(String.format(
                    "Initial deposit $%.2f is below the required minimum balance of $%.2f for %s.",
                    initialDeposit, accountType.getMinimumBalance(), accountType.getDisplayName()
            ));
        }

        // Find or create customer
        Customer customer = customerRepository.findByEmail(email).orElseGet(() -> {
            String custId = "CUST-" + customerSequence.incrementAndGet();
            Customer newCust = new Customer(custId, name.trim(), email.trim(), phone.trim(), tier);
            customerRepository.save(newCust);
            return newCust;
        });

        String accountNumber = "AC-" + accountSequence.incrementAndGet();
        Account account = new Account(accountNumber, customer.getCustomerId(), customer.getName(), accountType, 0.0);
        accountRepository.save(account);

        // Perform initial deposit if specified
        if (initialDeposit > 0) {
            deposit(accountNumber, initialDeposit, "Initial Account Opening Deposit");
        }

        return accountNumber;
    }

    @Override
    public synchronized Account getAccount(String accountNumber) {
        Validation.validateAccountNumber(accountNumber);
        return accountRepository.findByNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(accountNumber));
    }

    @Override
    public synchronized List<Account> listAllAccounts() {
        return accountRepository.findAll();
    }

    @Override
    public synchronized boolean closeAccount(String accountNumber) {
        Validation.validateAccountNumber(accountNumber);
        Account account = getAccount(accountNumber);
        if (account.getBalance() > 0.0) {
            throw new ValidationException(String.format(
                    "Cannot close account %s: account still holds a balance of $%.2f. Please withdraw funds first.",
                    accountNumber, account.getBalance()
            ));
        }
        account.setActive(false);
        accountRepository.save(account);
        return true;
    }

    // =========================================================================
    // FINANCIAL TRANSACTIONS
    // =========================================================================

    @Override
    public synchronized Transaction deposit(String accountNumber, double amount, String note) {
        Validation.validateAccountNumber(accountNumber);
        Validation.validatePositiveAmount(amount, "Deposit amount");
        Account account = getAccount(accountNumber);
        ensureActiveAccount(account);

        double newBalance = account.getBalance() + amount;
        account.setBalance(newBalance);
        accountRepository.save(account);

        Transaction tx = new Transaction(
                accountNumber, null, amount, TransactionType.DEPOSIT,
                (note == null || note.trim().isEmpty()) ? "Cash/Check Deposit" : note.trim(),
                newBalance
        );
        transactionRepository.save(tx);

        // Register Command Undo Action
        transactionRepository.getAuditStack().push(
                String.format("Undo Deposit of $%.2f to %s", amount, accountNumber),
                () -> {
                    Account acc = getAccount(accountNumber);
                    acc.setBalance(acc.getBalance() - amount);
                    accountRepository.save(acc);
                    transactionRepository.save(new Transaction(
                            accountNumber, null, amount, TransactionType.REVERSAL,
                            "Reversal of Deposit " + tx.getTransactionId(), acc.getBalance()
                    ));
                }
        );

        return tx;
    }

    @Override
    public synchronized Transaction withdraw(String accountNumber, double amount, String note) {
        Validation.validateAccountNumber(accountNumber);
        Validation.validatePositiveAmount(amount, "Withdrawal amount");
        Account account = getAccount(accountNumber);
        ensureActiveAccount(account);

        double minBalance = account.getAccountType().getMinimumBalance();
        if (account.getBalance() - amount < minBalance) {
            throw new InsufficientFundsException(String.format(
                    "Withdrawal denied for %s. Current balance is $%.2f; withdrawing $%.2f violates minimum required balance ($%.2f).",
                    accountNumber, account.getBalance(), amount, minBalance
            ));
        }

        double newBalance = account.getBalance() - amount;
        account.setBalance(newBalance);
        accountRepository.save(account);

        Transaction tx = new Transaction(
                accountNumber, null, amount, TransactionType.WITHDRAWAL,
                (note == null || note.trim().isEmpty()) ? "Cash Withdrawal" : note.trim(),
                newBalance
        );
        transactionRepository.save(tx);

        // Register Command Undo Action
        transactionRepository.getAuditStack().push(
                String.format("Undo Withdrawal of $%.2f from %s", amount, accountNumber),
                () -> {
                    Account acc = getAccount(accountNumber);
                    acc.setBalance(acc.getBalance() + amount);
                    accountRepository.save(acc);
                    transactionRepository.save(new Transaction(
                            accountNumber, null, amount, TransactionType.REVERSAL,
                            "Reversal of Withdrawal " + tx.getTransactionId(), acc.getBalance()
                    ));
                }
        );

        return tx;
    }

    @Override
    public synchronized List<Transaction> transfer(String fromAccount, String toAccount,
                                                  double amount, String note) {
        Validation.validateAccountNumber(fromAccount);
        Validation.validateAccountNumber(toAccount);
        Validation.validatePositiveAmount(amount, "Transfer amount");

        if (fromAccount.equalsIgnoreCase(toAccount)) {
            throw new ValidationException("Source and destination accounts cannot be identical.");
        }

        Account source = getAccount(fromAccount);
        Account dest = getAccount(toAccount);

        ensureActiveAccount(source);
        ensureActiveAccount(dest);

        double minBalance = source.getAccountType().getMinimumBalance();
        if (source.getBalance() - amount < minBalance) {
            throw new InsufficientFundsException(String.format(
                    "Transfer failed from %s: Current balance $%.2f is insufficient to transfer $%.2f while retaining required minimum balance ($%.2f).",
                    fromAccount, source.getBalance(), amount, minBalance
            ));
        }

        // Execute atomic state transitions
        source.setBalance(source.getBalance() - amount);
        dest.setBalance(dest.getBalance() + amount);

        accountRepository.save(source);
        accountRepository.save(dest);

        String transferNote = (note == null || note.trim().isEmpty()) ? "Direct Fund Transfer" : note.trim();

        Transaction txOut = new Transaction(
                fromAccount, toAccount, amount, TransactionType.TRANSFER_OUT,
                "Transfer to " + toAccount + ": " + transferNote, source.getBalance()
        );
        Transaction txIn = new Transaction(
                toAccount, fromAccount, amount, TransactionType.TRANSFER_IN,
                "Transfer from " + fromAccount + ": " + transferNote, dest.getBalance()
        );

        transactionRepository.save(txOut);
        transactionRepository.save(txIn);

        // Register Command Undo Action
        transactionRepository.getAuditStack().push(
                String.format("Undo Transfer of $%.2f from %s to %s", amount, fromAccount, toAccount),
                () -> {
                    Account src = getAccount(fromAccount);
                    Account dst = getAccount(toAccount);
                    src.setBalance(src.getBalance() + amount);
                    dst.setBalance(dst.getBalance() - amount);
                    accountRepository.save(src);
                    accountRepository.save(dst);

                    transactionRepository.save(new Transaction(
                            fromAccount, toAccount, amount, TransactionType.REVERSAL,
                            "Rollback Transfer OUT to " + toAccount, src.getBalance()
                    ));
                    transactionRepository.save(new Transaction(
                            toAccount, fromAccount, amount, TransactionType.REVERSAL,
                            "Rollback Transfer IN from " + fromAccount, dst.getBalance()
                    ));
                }
        );

        return Arrays.asList(txOut, txIn);
    }

    @Override
    public synchronized boolean undoLastOperation() {
        Optional<com.bank.dsa.TransactionAuditStack.ReversibleTransaction> op =
                transactionRepository.getAuditStack().pop();
        if (op.isEmpty()) {
            return false;
        }
        op.get().executeUndo();
        return true;
    }

    @Override
    public synchronized List<Transaction> getAccountStatement(String accountNumber) {
        Validation.validateAccountNumber(accountNumber);
        getAccount(accountNumber); // Verify existence
        return transactionRepository.findByAccountNumber(accountNumber);
    }

    // =========================================================================
    // DSA SEARCH & ANALYTICS
    // =========================================================================

    @Override
    public synchronized List<Customer> searchCustomersByPrefix(String prefix) {
        if (prefix == null || prefix.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return customerRepository.searchByNamePrefix(prefix.trim());
    }

    @Override
    public synchronized List<Account> findAccountsByBalanceRange(double minBalance, double maxBalance) {
        Validation.validateBalanceRange(minBalance, maxBalance);
        return accountRepository.findByBalanceRange(minBalance, maxBalance);
    }

    @Override
    public synchronized List<Account> searchAccountsByCustomerName(String query) {
        if (query == null || query.trim().isEmpty()) {
            return Collections.emptyList();
        }
        List<Customer> matchingCustomers = customerRepository.searchByNamePrefix(query.trim());
        Set<String> customerIds = matchingCustomers.stream()
                .map(Customer::getCustomerId)
                .collect(Collectors.toSet());

        return accountRepository.findAll().stream()
                .filter(a -> customerIds.contains(a.getCustomerId()) ||
                             a.getCustomerName().toLowerCase().contains(query.trim().toLowerCase()))
                .sorted(Comparator.comparing(Account::getAccountNumber))
                .collect(Collectors.toList());
    }

    // =========================================================================
    // PRIORITY CUSTOMER SERVICE DESK (MAX-HEAP)
    // =========================================================================

    @Override
    public synchronized ServiceRequest submitServiceRequest(String customerId, String queryDetails) {
        if (customerId == null || customerId.trim().isEmpty()) {
            throw new ValidationException("Customer ID cannot be empty or whitespace.");
        }
        Customer customer = customerRepository.findById(customerId.trim())
                .orElseThrow(() -> new ValidationException("Customer ID not found in system: " + customerId.trim()));

        Validation.validateText(queryDetails, "Support query details", 3, 500);

        ServiceRequest request = new ServiceRequest(
                customer.getCustomerId(),
                customer.getName(),
                customer.getTier(),
                queryDetails.trim()
        );

        priorityDeskQueue.insert(request);
        return request;
    }

    @Override
    public synchronized Optional<ServiceRequest> peekNextPriorityRequest() {
        return priorityDeskQueue.peekMax();
    }

    @Override
    public synchronized Optional<ServiceRequest> processNextPriorityRequest() {
        return priorityDeskQueue.extractMax();
    }

    @Override
    public synchronized List<ServiceRequest> listPendingServiceRequests() {
        List<ServiceRequest> heapCopy = priorityDeskQueue.toList();
        heapCopy.sort(Comparator.reverseOrder());
        return heapCopy;
    }

    // =========================================================================
    // AML & GRAPH FORENSIC ANALYSIS (BFS & DFS)
    // =========================================================================

    @Override
    public synchronized List<String> traceMoneyTrail(String fromAccount, String toAccount) {
        Validation.validateAccountNumber(fromAccount);
        Validation.validateAccountNumber(toAccount);
        getAccount(fromAccount); // Verify existence
        getAccount(toAccount);   // Verify existence
        return transactionRepository.getMoneyFlowGraph().findShortestTransferPath(fromAccount, toAccount);
    }

    @Override
    public synchronized List<List<String>> detectSuspiciousCycles(String accountNumber) {
        Validation.validateAccountNumber(accountNumber);
        getAccount(accountNumber); // Verify existence
        return transactionRepository.getMoneyFlowGraph().detectCircularFlows(accountNumber);
    }

    // =========================================================================
    // TELEMETRY & SAMPLE SEED DATA
    // =========================================================================

    @Override
    public synchronized String getCacheDiagnostics() {
        return accountRepository.getCache().getDiagnosticSummary();
    }

    @Override
    public synchronized void seedDemoData() {
        if (accountRepository.count() > 0) {
            return; // Already populated
        }

        // 1. Seed Diverse Customers across VIP Tiers
        Customer c1 = new Customer("CUST-1001", "Alice Smith", "alice.smith@email.com", "555-0101", CustomerTier.PLATINUM);
        Customer c2 = new Customer("CUST-1002", "Alexander Wright", "alex.w@email.com", "555-0102", CustomerTier.GOLD);
        Customer c3 = new Customer("CUST-1003", "Bob Johnson", "bob.j@email.com", "555-0103", CustomerTier.SILVER);
        Customer c4 = new Customer("CUST-1004", "Catherine Davis", "catherine.d@email.com", "555-0104", CustomerTier.PLATINUM);
        Customer c5 = new Customer("CUST-1005", "David Miller", "david.m@email.com", "555-0105", CustomerTier.REGULAR);
        Customer c6 = new Customer("CUST-1006", "Emma Watson", "emma.w@email.com", "555-0106", CustomerTier.REGULAR);

        customerRepository.save(c1);
        customerRepository.save(c2);
        customerRepository.save(c3);
        customerRepository.save(c4);
        customerRepository.save(c5);
        customerRepository.save(c6);

        // 2. Open Accounts with varied balances for BST Range Queries
        String acc1 = "AC-100001";
        String acc2 = "AC-100002";
        String acc3 = "AC-100003";
        String acc4 = "AC-100004";
        String acc5 = "AC-100005";
        String acc6 = "AC-100006";

        accountRepository.save(new Account(acc1, c1.getCustomerId(), c1.getName(), AccountType.SAVINGS, 12500.00));
        accountRepository.save(new Account(acc2, c2.getCustomerId(), c2.getName(), AccountType.CHECKING, 4200.00));
        accountRepository.save(new Account(acc3, c3.getCustomerId(), c3.getName(), AccountType.SAVINGS, 850.00));
        accountRepository.save(new Account(acc4, c4.getCustomerId(), c4.getName(), AccountType.FIXED_DEPOSIT, 50000.00));
        accountRepository.save(new Account(acc5, c5.getCustomerId(), c5.getName(), AccountType.CHECKING, 150.00));
        accountRepository.save(new Account(acc6, c6.getCustomerId(), c6.getName(), AccountType.SAVINGS, 3400.00));

        // 3. Seed Transactions & Transfers to form Graph Paths and an AML Circular Ring:
        transfer(acc1, acc2, 1000.00, "Consulting fee payment");
        transfer(acc2, acc3, 750.00, "Subcontractor payout");
        transfer(acc3, acc1, 500.00, "Refund balance (Cycle back)");

        transfer(acc2, acc5, 300.00, "Personal loan repayment");
        transfer(acc5, acc6, 200.00, "Gift transfer");

        // 4. Seed Support Tickets into Priority Max-Heap Queue
        priorityDeskQueue.insert(new ServiceRequest(c5.getCustomerId(), c5.getName(), c5.getTier(), "Inquiry regarding ATM fees"));
        priorityDeskQueue.insert(new ServiceRequest(c3.getCustomerId(), c3.getName(), c3.getTier(), "Requesting credit card upgrade"));
        priorityDeskQueue.insert(new ServiceRequest(c1.getCustomerId(), c1.getName(), c1.getTier(), "VIP Private Wealth portfolio consultation"));
        priorityDeskQueue.insert(new ServiceRequest(c4.getCustomerId(), c4.getName(), c4.getTier(), "High-value wire transfer pre-approval"));
    }

    private void ensureActiveAccount(Account account) {
        if (!account.isActive()) {
            throw new AccountFrozenException(account.getAccountNumber());
        }
    }
}
