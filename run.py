#!/usr/bin/env python3
"""
================================================================================
Enterprise CLI Banking System (DSA-Powered) - Python Execution Engine
================================================================================
A native Python implementation mirroring the Core Java architecture with robust
enterprise-grade validation and error handling for emails, phone numbers,
names, monetary amounts, and account lookups.

Features 6 custom Data Structures & Algorithms:
1. Trie (Prefix Tree) for instant customer name autocomplete
2. LRU Cache (Doubly Linked List + Hash Map) for hot account in-memory caching
3. Binary Search Tree (with branch-pruning range queries) for account balances
4. Binary Max-Heap for VIP Priority customer support triage
5. Directed Money Flow Graph for AML cycle detection (DFS) & shortest path (BFS)
6. LIFO Stack (Command Pattern) for transactional undo & rollback
================================================================================
"""

import sys
import re
import uuid
import datetime
from collections import deque

# --- ANSI Colors ---
class Color:
    RESET = "\033[0m"
    BOLD = "\033[1m"
    DIM = "\033[2m"
    RED = "\033[31m"
    GREEN = "\033[32m"
    YELLOW = "\033[33m"
    BLUE = "\033[34m"
    CYAN = "\033[36m"
    BRIGHT_RED = "\033[91m"
    BRIGHT_GREEN = "\033[92m"
    BRIGHT_CYAN = "\033[96m"

    @classmethod
    def wrap(cls, color, text):
        return f"{color}{text}{cls.RESET}"

# ==============================================================================
# ENTERPRISE VALIDATION UTILITIES
# ==============================================================================

class Validation:
    EMAIL_REGEX = re.compile(r'^[a-zA-Z0-9_+&*-]+(?:\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\.)+[a-zA-Z]{2,7}$')
    PHONE_REGEX = re.compile(r'^\+?[0-9\s()\-.]+?$')
    ACCOUNT_REGEX = re.compile(r'^AC-[0-9]{4,8}$', re.IGNORECASE)

    @staticmethod
    def validate_name(name):
        if not name or not name.strip():
            raise ValueError("Customer name cannot be empty or blank.")
        clean = name.strip()
        if len(clean) < 2:
            raise ValueError("Customer name is too short (minimum 2 characters required).")
        if len(clean) > 100:
            raise ValueError("Customer name exceeds maximum limit of 100 characters.")
        if not re.match(r"^[a-zA-Z\s.'-]+$", clean):
            raise ValueError("Customer name contains invalid characters. Only letters, spaces, hyphens, and apostrophes are allowed.")
        return clean

    @staticmethod
    def validate_email(email):
        if not email or not email.strip():
            raise ValueError("Email address cannot be empty or blank.")
        clean = email.strip()

        if " " in clean:
            raise ValueError("Invalid email: email address cannot contain spaces.")

        at_count = clean.count("@")
        if at_count == 0:
            raise ValueError("Invalid email format: missing '@' symbol (e.g. user@domain.com).")
        if at_count > 1:
            raise ValueError("Invalid email format: contains multiple '@' symbols.")

        local_part, domain_part = clean.split("@", 1)

        if not local_part:
            raise ValueError("Invalid email format: missing username/local part before '@'.")
        if not domain_part:
            raise ValueError("Invalid email format: missing domain name after '@' (e.g. domain.com).")

        if "." not in domain_part:
            raise ValueError("Invalid email domain: missing dot extension (e.g. .com, .org).")

        tld = domain_part.rsplit(".", 1)[1]
        if len(tld) < 2:
            raise ValueError(f"Invalid email domain: TLD extension '.{tld}' is too short (minimum 2 characters).")

        if not Validation.EMAIL_REGEX.match(clean):
            raise ValueError(f"Invalid email syntax: '{clean}'. Must be a valid address such as user@example.com.")
        return clean

    @staticmethod
    def validate_phone(phone):
        if not phone or not phone.strip():
            raise ValueError("Phone number cannot be empty.")
        clean = phone.strip()
        digits = [c for c in clean if c.isdigit()]
        if len(digits) < 7:
            raise ValueError(f"Phone number contains too few digits ({len(digits)} found; minimum 7 required).")
        if len(digits) > 15:
            raise ValueError(f"Phone number exceeds maximum length of 15 digits (ITU-T E.164 standard).")
        if not Validation.PHONE_REGEX.match(clean):
            raise ValueError("Phone number contains invalid characters (allowed: +, digits, spaces, hyphens).")
        return clean

    @staticmethod
    def validate_account_number(acc_num):
        if not acc_num or not acc_num.strip():
            raise ValueError("Account number cannot be empty or blank.")
        clean = acc_num.strip().upper()
        if not Validation.ACCOUNT_REGEX.match(clean):
            raise ValueError(f"Invalid account number format: '{acc_num}'. Format must be AC-XXXXXX (e.g. AC-100001).")
        return clean

    @staticmethod
    def validate_positive_amount(amount, field_name="Amount"):
        try:
            val = float(amount)
        except (ValueError, TypeError):
            raise ValueError(f"{field_name} must be a valid numeric number.")
        import math
        if math.isnan(val) or math.isinf(val):
            raise ValueError(f"{field_name} must be a finite number.")
        if val <= 0.0:
            raise ValueError(f"{field_name} must be strictly positive (greater than $0.00). Received: ${val:.2f}")
        if val > 100_000_000.0:
            raise ValueError(f"{field_name} exceeds the maximum single transaction limit of $100,000,000.00.")
        return val

    @staticmethod
    def validate_balance_range(min_val, max_val):
        Validation.validate_positive_amount(min_val + 1, "Minimum balance")
        Validation.validate_positive_amount(max_val + 1, "Maximum balance")
        if min_val < 0.0:
            raise ValueError("Minimum balance boundary cannot be negative.")
        if max_val < min_val:
            raise ValueError(f"Invalid balance interval: minimum boundary (${min_val:.2f}) cannot exceed maximum boundary (${max_val:.2f}).")

# --- Table Printer ---
class TablePrinter:
    def __init__(self, *headers):
        self.headers = list(headers)
        self.rows = []
        self.title = None
        self.right_align = [False] * len(headers)

    def set_title(self, title):
        self.title = title
        return self

    def set_right_align(self, idx, align=True):
        if 0 <= idx < len(self.right_align):
            self.right_align[idx] = align
        return self

    def add_row(self, *row):
        formatted_row = [str(val) if val is not None else "" for val in row]
        self.rows.append(formatted_row)
        return self

    def _strip_ansi(self, s):
        return re.sub(r'\033\[[;0-9]*m', '', str(s))

    def render(self):
        col_count = len(self.headers)
        col_widths = [len(self._strip_ansi(h)) for h in self.headers]

        for row in self.rows:
            for i in range(col_count):
                val = row[i] if i < len(row) else ""
                col_widths[i] = max(col_widths[i], len(self._strip_ansi(val)))

        col_widths = [w + 2 for w in col_widths]
        total_width = sum(col_widths) + col_count + 1

        lines = []
        sep = "+" + "+".join("-" * w for w in col_widths) + "+"
        header_sep = "+" + "+".join("=" * w for w in col_widths) + "+"

        if self.title:
            lines.append(sep)
            visible_len = len(self._strip_ansi(self.title))
            pad_left = (total_width - 2 - visible_len) // 2
            pad_right = total_width - 2 - visible_len - pad_left
            lines.append("|" + " " * pad_left + Color.wrap(Color.BOLD, self.title) + " " * pad_right + "|")

        lines.append(sep)
        h_cells = []
        for i, h in enumerate(self.headers):
            pad = col_widths[i] - len(self._strip_ansi(h)) - 2
            h_cells.append(" " + Color.wrap(Color.BOLD, h) + " " * (pad + 1))
        lines.append("|" + "|".join(h_cells) + "|")
        lines.append(header_sep)

        if not self.rows:
            empty_msg = "(No records found)"
            pad_left = (total_width - 2 - len(empty_msg)) // 2
            pad_right = total_width - 2 - len(empty_msg) - pad_left
            lines.append("|" + " " * pad_left + Color.wrap(Color.DIM, empty_msg) + " " * pad_right + "|")
        else:
            for row in self.rows:
                cells = []
                for i in range(col_count):
                    val = row[i] if i < len(row) else ""
                    vis_len = len(self._strip_ansi(val))
                    pad = max(0, col_widths[i] - vis_len - 2)
                    if self.right_align[i]:
                        cells.append(" " + " " * pad + val + " ")
                    else:
                        cells.append(" " + val + " " * pad + " ")
                lines.append("|" + "|".join(cells) + "|")

        lines.append(sep)
        return "\n".join(lines)

    def print(self):
        print(self.render())

# ==============================================================================
# DATA STRUCTURES
# ==============================================================================

# 1. TRIE (Prefix Tree)
class TrieNode:
    def __init__(self):
        self.children = {}
        self.values = set()
        self.is_end = False

class Trie:
    def __init__(self):
        self.root = TrieNode()
        self.size = 0

    def insert(self, key, value):
        if not key or not value:
            return
        self._insert_token(key.strip().lower(), value)
        for token in key.strip().lower().split():
            if token:
                self._insert_token(token, value)

    def _insert_token(self, token, value):
        curr = self.root
        for ch in token:
            if ch not in curr.children:
                curr.children[ch] = TrieNode()
            curr = curr.children[ch]
            curr.values.add(value)
        if not curr.is_end:
            curr.is_end = True
            self.size += 1

    def search_prefix(self, prefix):
        if not prefix:
            return []
        curr = self.root
        for ch in prefix.strip().lower():
            if ch not in curr.children:
                return []
            curr = curr.children[ch]
        return list(curr.values)

# 2. LRU CACHE (Doubly Linked List + Hash Map)
class DListNode:
    def __init__(self, key=None, value=None):
        self.key = key
        self.value = value
        self.prev = None
        self.next = None

class LRUCache:
    def __init__(self, capacity=50):
        self.capacity = capacity
        self.map = {}
        self.head = DListNode()
        self.tail = DListNode()
        self.head.next = self.tail
        self.tail.prev = self.head
        self.hits = 0
        self.misses = 0
        self.evictions = 0

    def get(self, key):
        if key in self.map:
            self.hits += 1
            node = self.map[key]
            self._move_to_head(node)
            return node.value
        self.misses += 1
        return None

    def put(self, key, value):
        if key in self.map:
            node = self.map[key]
            node.value = value
            self._move_to_head(node)
            return
        new_node = DListNode(key, value)
        self.map[key] = new_node
        self._add_head(new_node)
        if len(self.map) > self.capacity:
            lru = self._remove_tail()
            if lru:
                del self.map[lru.key]
                self.evictions += 1

    def _add_head(self, node):
        node.prev = self.head
        node.next = self.head.next
        self.head.next.prev = node
        self.head.next = node

    def _remove_node(self, node):
        node.prev.next = node.next
        node.next.prev = node.prev

    def _move_to_head(self, node):
        self._remove_node(node)
        self._add_head(node)

    def _remove_tail(self):
        node = self.tail.prev
        if node == self.head:
            return None
        self._remove_node(node)
        return node

    def get_hit_rate(self):
        total = self.hits + self.misses
        return (self.hits / total * 100.0) if total > 0 else 0.0

    def get_summary(self):
        return (f"LRU Cache Status: Size={len(self.map)}/{self.capacity} | "
                f"Hits={self.hits} | Misses={self.misses} | "
                f"Hit Rate={self.get_hit_rate():.1f}% | Evictions={self.evictions}")

# 3. BINARY SEARCH TREE (Range Search)
class BSTNode:
    def __init__(self, key, value):
        self.key = key
        self.values = [value]
        self.left = None
        self.right = None

class BinarySearchTree:
    def __init__(self):
        self.root = None
        self.size = 0

    def insert(self, key, value):
        self.root = self._insert(self.root, key, value)
        self.size += 1

    def _insert(self, node, key, value):
        if not node:
            return BSTNode(key, value)
        if key < node.key:
            node.left = self._insert(node.left, key, value)
        elif key > node.key:
            node.right = self._insert(node.right, key, value)
        else:
            node.values.append(value)
        return node

    def remove(self, key, value):
        self.root = self._remove(self.root, key, value)

    def _remove(self, node, key, value):
        if not node:
            return None
        if key < node.key:
            node.left = self._remove(node.left, key, value)
        elif key > node.key:
            node.right = self._remove(node.right, key, value)
        else:
            if value in node.values:
                node.values.remove(value)
                self.size -= 1
            if node.values:
                return node
            if not node.left:
                return node.right
            if not node.right:
                return node.left
            succ = self._min_node(node.right)
            node.key = succ.key
            node.values = list(succ.values)
            node.right = self._remove_all_key(node.right, succ.key)
        return node

    def _min_node(self, node):
        curr = node
        while curr.left:
            curr = curr.left
        return curr

    def _remove_all_key(self, node, key):
        if not node:
            return None
        if key < node.key:
            node.left = self._remove_all_key(node.left, key)
        elif key > node.key:
            node.right = self._remove_all_key(node.right, key)
        else:
            if not node.left: return node.right
            if not node.right: return node.left
            succ = self._min_node(node.right)
            node.key = succ.key
            node.values = list(succ.values)
            node.right = self._remove_all_key(node.right, succ.key)
        return node

    def find_range(self, min_val, max_val):
        results = []
        self._range_search(self.root, min_val, max_val, results)
        return results

    def _range_search(self, node, min_val, max_val, results):
        if not node:
            return
        if min_val is None or node.key > min_val:
            self._range_search(node.left, min_val, max_val, results)
        if (min_val is None or node.key >= min_val) and (max_val is None or node.key <= max_val):
            results.extend(node.values)
        if max_val is None or node.key < max_val:
            self._range_search(node.right, min_val, max_val, results)

# 4. BINARY MAX-HEAP (Priority Queue)
class MaxHeap:
    def __init__(self):
        self.heap = []

    def insert(self, item):
        self.heap.append(item)
        self._sift_up(len(self.heap) - 1)

    def extract_max(self):
        if not self.heap:
            return None
        max_item = self.heap[0]
        last_item = self.heap.pop()
        if self.heap:
            self.heap[0] = last_item
            self._sift_down(0)
        return max_item

    def peek_max(self):
        return self.heap[0] if self.heap else None

    def _sift_up(self, idx):
        while idx > 0:
            parent = (idx - 1) // 2
            if self.heap[idx] > self.heap[parent]:
                self.heap[idx], self.heap[parent] = self.heap[parent], self.heap[idx]
                idx = parent
            else:
                break

    def _sift_down(self, idx):
        size = len(self.heap)
        while idx < size:
            left = 2 * idx + 1
            right = 2 * idx + 2
            largest = idx
            if left < size and self.heap[left] > self.heap[largest]:
                largest = left
            if right < size and self.heap[right] > self.heap[largest]:
                largest = right
            if largest != idx:
                self.heap[idx], self.heap[largest] = self.heap[largest], self.heap[idx]
                idx = largest
            else:
                break

    def to_sorted_list(self):
        return sorted(self.heap, reverse=True)

# 5. DIRECTED MONEY FLOW GRAPH (BFS & DFS AML)
class MoneyFlowGraph:
    def __init__(self):
        self.adj = {}

    def add_transfer(self, from_acc, to_acc, amount, tx_id):
        if not from_acc or not to_acc or from_acc == to_acc:
            return
        self.adj.setdefault(from_acc, []).append({'to': to_acc, 'amount': amount, 'tx_id': tx_id})
        self.adj.setdefault(to_acc, [])

    def shortest_path_bfs(self, start, target):
        if start not in self.adj or target not in self.adj:
            return []
        if start == target:
            return [start]
        queue = deque([start])
        visited = {start}
        parent = {}
        found = False

        while queue:
            curr = queue.popleft()
            if curr == target:
                found = True
                break
            for edge in self.adj.get(curr, []):
                nbr = edge['to']
                if nbr not in visited:
                    visited.add(nbr)
                    parent[nbr] = curr
                    queue.append(nbr)

        if not found:
            return []
        path = []
        c = target
        while c:
            path.append(c)
            c = parent.get(c)
        return path[::-1]

    def detect_cycles_dfs(self, start_acc):
        if start_acc not in self.adj:
            return []
        detected_cycles = []
        path = [start_acc]
        visited = set()

        def dfs(curr, depth):
            if depth > 10:
                return
            for edge in self.adj.get(curr, []):
                nbr = edge['to']
                if nbr == start_acc and len(path) > 1:
                    detected_cycles.append(list(path) + [start_acc])
                elif nbr not in visited and nbr not in path:
                    visited.add(nbr)
                    path.append(nbr)
                    dfs(nbr, depth + 1)
                    path.pop()
                    visited.remove(nbr)

        dfs(start_acc, 0)
        return detected_cycles

# ==============================================================================
# DOMAIN MODELS
# ==============================================================================

class CustomerTier:
    REGULAR = (1, "Regular")
    SILVER = (2, "Silver")
    GOLD = (3, "Gold")
    PLATINUM = (4, "Platinum VIP")

class Customer:
    def __init__(self, cust_id, name, email, phone="N/A", tier=CustomerTier.REGULAR):
        self.cust_id = cust_id
        self.name = name
        self.email = email
        self.phone = phone
        self.tier = tier

class Account:
    def __init__(self, acc_num, cust_id, cust_name, acc_type="SAVINGS", balance=0.0):
        self.acc_num = acc_num
        self.cust_id = cust_id
        self.cust_name = cust_name
        self.acc_type = acc_type
        self.balance = float(balance)
        self.active = True

class Transaction:
    def __init__(self, acc_num, counterpart, amount, tx_type, note, balance_after):
        self.tx_id = "TX-" + uuid.uuid4().hex[:8].upper()
        self.acc_num = acc_num
        self.counterpart = counterpart
        self.amount = float(amount)
        self.tx_type = tx_type
        self.note = note
        self.balance_after = float(balance_after)
        self.timestamp = datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S")

class ServiceRequest:
    def __init__(self, cust_id, cust_name, tier, query):
        self.req_id = "REQ-" + uuid.uuid4().hex[:8].upper()
        self.cust_id = cust_id
        self.cust_name = cust_name
        self.tier = tier
        self.query = query
        self.timestamp = datetime.datetime.now()

    def __lt__(self, other):
        if self.tier[0] != other.tier[0]:
            return self.tier[0] < other.tier[0]
        return self.timestamp > other.timestamp

# ==============================================================================
# BANK SERVICE
# ==============================================================================

class BankService:
    def __init__(self):
        self.accounts = {}
        self.customers = {}
        self.transactions = []
        self.tx_by_acc = {}

        # DSA Indexes
        self.name_trie = Trie()
        self.account_cache = LRUCache(50)
        self.balance_bst = BinarySearchTree()
        self.priority_desk = MaxHeap()
        self.money_graph = MoneyFlowGraph()
        self.undo_stack = []

        self.acc_seq = 100000
        self.cust_seq = 1000

    def open_account(self, name, email, phone, acc_type="SAVINGS", initial_deposit=0.0, tier=CustomerTier.REGULAR):
        name = Validation.validate_name(name)
        email = Validation.validate_email(email)
        phone = Validation.validate_phone(phone)

        min_bal = 100.0 if acc_type == "SAVINGS" else 500.0 if acc_type == "FIXED_DEPOSIT" else 0.0
        if initial_deposit < min_bal:
            raise ValueError(f"Initial deposit (${initial_deposit:.2f}) is below the required minimum of ${min_bal:.2f} for {acc_type}.")

        cust = next((c for c in self.customers.values() if c.email.lower() == email.lower()), None)
        if not cust:
            self.cust_seq += 1
            cust_id = f"CUST-{self.cust_seq}"
            cust = Customer(cust_id, name, email, phone, tier)
            self.customers[cust_id] = cust
            self.name_trie.insert(name, cust)

        self.acc_seq += 1
        acc_num = f"AC-{self.acc_seq}"
        account = Account(acc_num, cust.cust_id, cust.name, acc_type, 0.0)
        self.accounts[acc_num] = account
        self.account_cache.put(acc_num, account)
        self.balance_bst.insert(account.balance, account)

        if initial_deposit > 0:
            self.deposit(acc_num, initial_deposit, "Initial Account Opening Deposit")

        return acc_num

    def get_account(self, acc_num):
        acc_num = Validation.validate_account_number(acc_num)
        cached = self.account_cache.get(acc_num)
        if cached:
            return cached
        if acc_num in self.accounts:
            acc = self.accounts[acc_num]
            self.account_cache.put(acc_num, acc)
            return acc
        return None

    def deposit(self, acc_num, amount, note="Deposit"):
        acc_num = Validation.validate_account_number(acc_num)
        amount = Validation.validate_positive_amount(amount, "Deposit amount")
        acc = self.get_account(acc_num)
        if not acc:
            raise ValueError(f"Account not found: {acc_num}")
        if not acc.active:
            raise ValueError(f"Account {acc_num} is frozen or closed.")

        old_bal = acc.balance
        self.balance_bst.remove(old_bal, acc)
        acc.balance += amount
        self.balance_bst.insert(acc.balance, acc)
        self.account_cache.put(acc_num, acc)

        tx = Transaction(acc_num, None, amount, "DEPOSIT", note or "Deposit", acc.balance)
        self.transactions.append(tx)
        self.tx_by_acc.setdefault(acc_num, []).append(tx)

        def undo_action():
            a = self.get_account(acc_num)
            self.balance_bst.remove(a.balance, a)
            a.balance -= amount
            self.balance_bst.insert(a.balance, a)
            self.account_cache.put(acc_num, a)
            rev_tx = Transaction(acc_num, None, amount, "REVERSAL", f"Rollback Deposit {tx.tx_id}", a.balance)
            self.transactions.append(rev_tx)
            self.tx_by_acc.setdefault(acc_num, []).append(rev_tx)

        self.undo_stack.append((f"Deposit of ${amount:.2f} to {acc_num}", undo_action))
        return tx

    def withdraw(self, acc_num, amount, note="Withdrawal"):
        acc_num = Validation.validate_account_number(acc_num)
        amount = Validation.validate_positive_amount(amount, "Withdrawal amount")
        acc = self.get_account(acc_num)
        if not acc:
            raise ValueError(f"Account not found: {acc_num}")
        if not acc.active:
            raise ValueError(f"Account {acc_num} is frozen or closed.")

        min_bal = 100.0 if acc.acc_type == "SAVINGS" else 0.0
        if acc.balance - amount < min_bal:
            raise ValueError(f"Insufficient funds: Balance is ${acc.balance:.2f}; withdrawing ${amount:.2f} violates minimum required balance (${min_bal:.2f}).")

        old_bal = acc.balance
        self.balance_bst.remove(old_bal, acc)
        acc.balance -= amount
        self.balance_bst.insert(acc.balance, acc)
        self.account_cache.put(acc_num, acc)

        tx = Transaction(acc_num, None, amount, "WITHDRAWAL", note or "Withdrawal", acc.balance)
        self.transactions.append(tx)
        self.tx_by_acc.setdefault(acc_num, []).append(tx)

        def undo_action():
            a = self.get_account(acc_num)
            self.balance_bst.remove(a.balance, a)
            a.balance += amount
            self.balance_bst.insert(a.balance, a)
            self.account_cache.put(acc_num, a)
            rev_tx = Transaction(acc_num, None, amount, "REVERSAL", f"Rollback Withdrawal {tx.tx_id}", a.balance)
            self.transactions.append(rev_tx)
            self.tx_by_acc.setdefault(acc_num, []).append(rev_tx)

        self.undo_stack.append((f"Withdrawal of ${amount:.2f} from {acc_num}", undo_action))
        return tx

    def transfer(self, from_acc, to_acc, amount, note="Direct Transfer"):
        from_acc = Validation.validate_account_number(from_acc)
        to_acc = Validation.validate_account_number(to_acc)
        if from_acc.upper() == to_acc.upper():
            raise ValueError("Source and destination accounts cannot be identical.")

        amount = Validation.validate_positive_amount(amount, "Transfer amount")
        src = self.get_account(from_acc)
        dst = self.get_account(to_acc)
        if not src:
            raise ValueError(f"Source account {from_acc} does not exist.")
        if not dst:
            raise ValueError(f"Destination account {to_acc} does not exist.")
        if not src.active or not dst.active:
            raise ValueError("One or both accounts are inactive/frozen.")

        min_bal = 100.0 if src.acc_type == "SAVINGS" else 0.0
        if src.balance - amount < min_bal:
            raise ValueError(f"Insufficient funds in source account {from_acc}: Current balance is ${src.balance:.2f}; minimum required is ${min_bal:.2f}.")

        self.balance_bst.remove(src.balance, src)
        self.balance_bst.remove(dst.balance, dst)
        src.balance -= amount
        dst.balance += amount
        self.balance_bst.insert(src.balance, src)
        self.balance_bst.insert(dst.balance, dst)
        self.account_cache.put(from_acc, src)
        self.account_cache.put(to_acc, dst)

        transfer_note = note if note and note.strip() else "Direct Fund Transfer"
        tx_out = Transaction(from_acc, to_acc, amount, "TRANSFER_OUT", f"Transfer to {to_acc}: {transfer_note}", src.balance)
        tx_in = Transaction(to_acc, from_acc, amount, "TRANSFER_IN", f"Transfer from {from_acc}: {transfer_note}", dst.balance)
        self.transactions.extend([tx_out, tx_in])
        self.tx_by_acc.setdefault(from_acc, []).append(tx_out)
        self.tx_by_acc.setdefault(to_acc, []).append(tx_in)
        self.money_graph.add_transfer(from_acc, to_acc, amount, tx_out.tx_id)

        def undo_action():
            s = self.get_account(from_acc)
            d = self.get_account(to_acc)
            self.balance_bst.remove(s.balance, s)
            self.balance_bst.remove(d.balance, d)
            s.balance += amount
            d.balance -= amount
            self.balance_bst.insert(s.balance, s)
            self.balance_bst.insert(d.balance, d)
            self.account_cache.put(from_acc, s)
            self.account_cache.put(to_acc, d)
            r_out = Transaction(from_acc, to_acc, amount, "REVERSAL", f"Rollback Transfer OUT to {to_acc}", s.balance)
            r_in = Transaction(to_acc, from_acc, amount, "REVERSAL", f"Rollback Transfer IN from {from_acc}", d.balance)
            self.transactions.extend([r_out, r_in])
            self.tx_by_acc.setdefault(from_acc, []).append(r_out)
            self.tx_by_acc.setdefault(to_acc, []).append(r_in)

        self.undo_stack.append((f"Transfer of ${amount:.2f} from {from_acc} to {to_acc}", undo_action))
        return [tx_out, tx_in]

    def undo(self):
        if not self.undo_stack:
            return False, "No reversible operations found on the stack."
        desc, action = self.undo_stack.pop()
        action()
        return True, desc

    def seed_demo_data(self):
        if self.accounts:
            return
        c1 = Customer("CUST-1001", "Alice Smith", "alice.smith@email.com", "555-0101", CustomerTier.PLATINUM)
        c2 = Customer("CUST-1002", "Alexander Wright", "alex.w@email.com", "555-0102", CustomerTier.GOLD)
        c3 = Customer("CUST-1003", "Bob Johnson", "bob.j@email.com", "555-0103", CustomerTier.SILVER)
        c4 = Customer("CUST-1004", "Catherine Davis", "catherine.d@email.com", "555-0104", CustomerTier.PLATINUM)
        c5 = Customer("CUST-1005", "David Miller", "david.m@email.com", "555-0105", CustomerTier.REGULAR)
        c6 = Customer("CUST-1006", "Emma Watson", "emma.w@email.com", "555-0106", CustomerTier.REGULAR)

        for c in [c1, c2, c3, c4, c5, c6]:
            self.customers[c.cust_id] = c
            self.name_trie.insert(c.name, c)

        acc_data = [
            ("AC-100001", c1, "SAVINGS", 12500.0),
            ("AC-100002", c2, "CHECKING", 4200.0),
            ("AC-100003", c3, "SAVINGS", 850.0),
            ("AC-100004", c4, "FIXED_DEPOSIT", 50000.0),
            ("AC-100005", c5, "CHECKING", 150.0),
            ("AC-100006", c6, "SAVINGS", 3400.0),
        ]
        for acc_num, c, t, bal in acc_data:
            a = Account(acc_num, c.cust_id, c.name, t, bal)
            self.accounts[acc_num] = a
            self.account_cache.put(acc_num, a)
            self.balance_bst.insert(bal, a)

        # Create AML Cycle Ring: AC-100001 -> AC-100002 -> AC-100003 -> AC-100001
        self.transfer("AC-100001", "AC-100002", 1000.0, "Consulting fee payment")
        self.transfer("AC-100002", "AC-100003", 750.0, "Subcontractor payout")
        self.transfer("AC-100003", "AC-100001", 500.0, "Refund balance (Cycle back)")

        # Create outlying path: AC-100002 -> AC-100005 -> AC-100006
        self.transfer("AC-100002", "AC-100005", 300.0, "Personal loan repayment")
        self.transfer("AC-100005", "AC-100006", 200.0, "Gift transfer")

        # VIP Tickets
        self.priority_desk.insert(ServiceRequest(c5.cust_id, c5.name, c5.tier, "Inquiry regarding ATM fees"))
        self.priority_desk.insert(ServiceRequest(c3.cust_id, c3.name, c3.tier, "Requesting credit card upgrade"))
        self.priority_desk.insert(ServiceRequest(c1.cust_id, c1.name, c1.tier, "VIP Private Wealth portfolio consultation"))
        self.priority_desk.insert(ServiceRequest(c4.cust_id, c4.name, c4.tier, "High-value wire transfer pre-approval"))

# ==============================================================================
# CLI APPLICATION WITH INTERACTIVE ERROR LOOPS
# ==============================================================================

class ConsoleBankApp:
    def __init__(self):
        self.service = BankService()

    def run(self):
        self.print_banner()
        while True:
            self.print_menu()
            choice = input(Color.wrap(Color.BOLD, "Enter menu choice [0-12]: ")).strip()
            try:
                if choice == "1":   self.handle_open_account()
                elif choice == "2": self.handle_view_statement()
                elif choice == "3": self.handle_deposit()
                elif choice == "4": self.handle_withdraw()
                elif choice == "5": self.handle_transfer()
                elif choice == "6": self.handle_undo()
                elif choice == "7": self.handle_trie_search()
                elif choice == "8": self.handle_bst_range()
                elif choice == "9": self.handle_priority_desk()
                elif choice == "10": self.handle_aml_fraud()
                elif choice == "11": self.handle_diagnostics()
                elif choice == "12": self.handle_demo_data()
                elif choice == "0":
                    print(Color.wrap(Color.BRIGHT_CYAN, "\nThank you for using the DSA Banking System. Goodbye!"))
                    break
                else:
                    print(Color.wrap(Color.RED, "Invalid option. Please enter a number between 0 and 12."))
            except Exception as e:
                print(Color.wrap(Color.RED, f"\n[ERROR] {e}"))

            input(Color.wrap(Color.DIM, "\n[Press Enter to continue...]"))

    def print_banner(self):
        print(Color.wrap(Color.BRIGHT_CYAN, r"""
================================================================================
   ____            _        ____              _    _             ____  _     ___ 
  | __ )  __ _ _ __| | __   / ___| _   _  ___ | |_ | |__   ___  / ___|| |   |_ _|
  |  _ \ / _` | '__| |/ /   \___ \| | | |/ _ \| __|| '_ \ / _ \ | |    | |    | | 
  | |_) | (_| | |  |   <     ___) | |_| | (_) | |_ | |_) |  __/ | |___ | |___ | | 
  |____/ \__,_|_|  |_|\_\   |____/ \__, |\___/ \__||_.__/ \___|  \____||_____|___|
                                   |___/                                          
                 DSA-Powered Enterprise Banking System (CLI)
================================================================================
"""))

    def print_menu(self):
        print(Color.wrap(Color.BOLD, "\n========================== MAIN NAVIGATION MENU =========================="))
        print(f" {Color.wrap(Color.CYAN, '[ 1]')} Open New Account                   {Color.wrap(Color.CYAN, '[ 7]')} Search Customers (Trie Autocomplete)")
        print(f" {Color.wrap(Color.CYAN, '[ 2]')} View Account Statement             {Color.wrap(Color.CYAN, '[ 8]')} Range Search Balances (BST Query)")
        print(f" {Color.wrap(Color.CYAN, '[ 3]')} Deposit Funds                      {Color.wrap(Color.CYAN, '[ 9]')} VIP Priority Service Desk (Max-Heap)")
        print(f" {Color.wrap(Color.CYAN, '[ 4]')} Withdraw Funds                     {Color.wrap(Color.CYAN, '[10]')} AML Fraud & Money Trail (Graph BFS/DFS)")
        print(f" {Color.wrap(Color.CYAN, '[ 5]')} Transfer Funds                     {Color.wrap(Color.CYAN, '[11]')} Diagnostics & LRU Cache Telemetry")
        print(f" {Color.wrap(Color.CYAN, '[ 6]')} Undo Last Operation (Stack)        {Color.wrap(Color.CYAN, '[12]')} 1-Click Load Realistic Demo Dataset")
        print(f" {Color.wrap(Color.RED, '[ 0]')} Exit System")
        print(Color.wrap(Color.BOLD, "=========================================================================="))

    # --- Validating Prompt Helpers with Retry Loops ---

    def prompt_validated_name(self):
        while True:
            val = input("Customer Full Name: ").strip()
            try:
                return Validation.validate_name(val)
            except ValueError as e:
                print(Color.wrap(Color.RED, f"  ✖ [NAME ERROR] {e}"))

    def prompt_validated_email(self):
        while True:
            val = input("Email Address: ").strip()
            try:
                return Validation.validate_email(val)
            except ValueError as e:
                print(Color.wrap(Color.RED, f"  ✖ [EMAIL ERROR] {e}"))

    def prompt_validated_phone(self):
        while True:
            val = input("Phone Number: ").strip()
            try:
                return Validation.validate_phone(val)
            except ValueError as e:
                print(Color.wrap(Color.RED, f"  ✖ [PHONE ERROR] {e}"))

    def prompt_validated_account_number(self, prompt_text="Account Number: "):
        while True:
            val = input(prompt_text).strip()
            try:
                return Validation.validate_account_number(val)
            except ValueError as e:
                print(Color.wrap(Color.RED, f"  ✖ [ACCOUNT ERROR] {e}"))

    def prompt_positive_amount(self, prompt_text="Amount ($): "):
        while True:
            raw = input(prompt_text).strip()
            try:
                return Validation.validate_positive_amount(raw, "Amount")
            except ValueError as e:
                print(Color.wrap(Color.RED, f"  ✖ [AMOUNT ERROR] {e}"))

    def handle_open_account(self):
        print(Color.wrap(Color.BOLD, "\n--- OPEN NEW BANK ACCOUNT ---"))
        name = self.prompt_validated_name()
        email = self.prompt_validated_email()
        phone = self.prompt_validated_phone()

        print("Account Types: [1] SAVINGS (Min $100)  [2] CHECKING (Min $0)  [3] FIXED_DEPOSIT (Min $500)")
        tc = input("Select Type (1/2/3, default 1): ").strip()
        acc_type = {"2": "CHECKING", "3": "FIXED_DEPOSIT"}.get(tc, "SAVINGS")

        print("Customer Tiers: [1] REGULAR  [2] SILVER  [3] GOLD  [4] PLATINUM")
        trc = input("Select Tier (1/2/3/4, default 1): ").strip()
        tier = {"2": CustomerTier.SILVER, "3": CustomerTier.GOLD, "4": CustomerTier.PLATINUM}.get(trc, CustomerTier.REGULAR)

        min_bal = 100.0 if acc_type == "SAVINGS" else 500.0 if acc_type == "FIXED_DEPOSIT" else 0.0
        while True:
            raw_dep = input(f"Initial Deposit Amount (Min ${min_bal:.2f} for {acc_type}): $").strip()
            if not raw_dep:
                raw_dep = "0"
            try:
                dep_val = float(raw_dep)
                if dep_val < 0:
                    print(Color.wrap(Color.RED, "  ✖ Deposit cannot be negative."))
                    continue
                if dep_val < min_bal:
                    print(Color.wrap(Color.RED, f"  ✖ Deposit is below the minimum required of ${min_bal:.2f}."))
                    continue
                initial_deposit = dep_val
                break
            except ValueError:
                print(Color.wrap(Color.RED, "  ✖ Please enter a valid numerical amount."))

        acc_num = self.service.open_account(name, email, phone, acc_type, initial_deposit, tier)
        print(Color.wrap(Color.GREEN, f"\n✔ Account successfully created!"))
        print(f"  Account Number: {Color.wrap(Color.BRIGHT_CYAN, acc_num)}")
        print(Color.wrap(Color.DIM, "  [DSA: Indexed in Primary Map, LRU Cache, Balance BST, and Customer Name Trie]"))

    def handle_view_statement(self):
        print(Color.wrap(Color.BOLD, "\n--- ACCOUNT STATEMENT & AUDIT ---"))
        acc_num = self.prompt_validated_account_number("Enter Account Number: ")
        acc = self.service.get_account(acc_num)
        if not acc:
            print(Color.wrap(Color.RED, f"Account {acc_num} not found in system."))
            return

        table = TablePrinter("Property", "Value").set_title(f"Account Overview - {acc.acc_num}")
        table.add_row("Account Number", acc.acc_num)
        table.add_row("Account Holder", acc.cust_name)
        table.add_row("Account Type", acc.acc_type)
        table.add_row("Current Balance", f"${acc.balance:.2f}")
        table.add_row("Status", Color.wrap(Color.GREEN, "ACTIVE") if acc.active else Color.wrap(Color.RED, "CLOSED"))
        table.print()

        txs = self.service.tx_by_acc.get(acc_num, [])
        tx_table = TablePrinter("Tx ID", "Timestamp", "Type", "Amount", "Balance After", "Description").set_title("Transaction Statement")
        tx_table.set_right_align(3).set_right_align(4)
        for t in txs:
            color = Color.GREEN if "IN" in t.tx_type or "DEPOSIT" in t.tx_type else Color.RED if "OUT" in t.tx_type or "WITHDRAWAL" in t.tx_type else Color.YELLOW
            tx_table.add_row(t.tx_id, t.timestamp, t.tx_type, Color.wrap(color, f"${t.amount:.2f}"), f"${t.balance_after:.2f}", t.note)
        tx_table.print()

    def handle_deposit(self):
        print(Color.wrap(Color.BOLD, "\n--- DEPOSIT FUNDS ---"))
        acc = self.prompt_validated_account_number("Account Number: ")
        amt = self.prompt_positive_amount("Deposit Amount ($): ")
        note = input("Note (optional): ").strip()
        tx = self.service.deposit(acc, amt, note)
        print(Color.wrap(Color.GREEN, f"✔ Deposited ${amt:.2f} into {acc}. New Balance: ${tx.balance_after:.2f}"))
        print(Color.wrap(Color.DIM, "  [DSA: Pushed to Undo Stack for 1-click Rollback]"))

    def handle_withdraw(self):
        print(Color.wrap(Color.BOLD, "\n--- WITHDRAW FUNDS ---"))
        acc = self.prompt_validated_account_number("Account Number: ")
        amt = self.prompt_positive_amount("Withdrawal Amount ($): ")
        note = input("Note (optional): ").strip()
        tx = self.service.withdraw(acc, amt, note)
        print(Color.wrap(Color.GREEN, f"✔ Withdrew ${amt:.2f} from {acc}. New Balance: ${tx.balance_after:.2f}"))

    def handle_transfer(self):
        print(Color.wrap(Color.BOLD, "\n--- TRANSFER FUNDS ---"))
        src = self.prompt_validated_account_number("Source Account (From): ")
        while True:
            dst = self.prompt_validated_account_number("Destination Account (To): ")
            if dst.upper() == src.upper():
                print(Color.wrap(Color.RED, "  ✖ Source and destination accounts cannot be identical. Please enter a different target."))
                continue
            break

        amt = self.prompt_positive_amount("Transfer Amount ($): ")
        note = input("Transfer Note: ").strip()
        self.service.transfer(src, dst, amt, note)
        print(Color.wrap(Color.GREEN, f"✔ Successfully transferred ${amt:.2f} from {src} to {dst}."))
        print(Color.wrap(Color.DIM, "  [DSA: Graph Edge recorded in MoneyFlowGraph & pushed to Undo Stack]"))

    def handle_undo(self):
        print(Color.wrap(Color.BOLD, "\n--- UNDO LAST FINANCIAL OPERATION (LIFO STACK) ---"))
        success, desc = self.service.undo()
        if success:
            print(Color.wrap(Color.GREEN, f"✔ Successfully rolled back: {desc}"))
            print(Color.wrap(Color.DIM, "  [DSA IN ACTION: Stack.pop() executed Command Pattern reversal in O(1)]"))
        else:
            print(Color.wrap(Color.YELLOW, desc))

    def handle_trie_search(self):
        print(Color.wrap(Color.BOLD, "\n--- TRIE PREFIX SEARCH (INSTANT AUTOCOMPLETE) ---"))
        prefix = input("Enter Name Prefix (e.g. 'Al', 'Bo', 'Cat'): ").strip()
        if not prefix:
            print(Color.wrap(Color.YELLOW, "Search prefix cannot be blank."))
            return
        import time
        t0 = time.perf_counter()
        matches = self.service.name_trie.search_prefix(prefix)
        elapsed = (time.perf_counter() - t0) * 1000
        table = TablePrinter("Customer ID", "Name", "Email", "Phone", "Tier").set_title(f"Trie Autocomplete Matches for '{prefix}' ({len(matches)} found)")
        for c in matches:
            table.add_row(c.cust_id, c.name, c.email, c.phone, c.tier[1])
        table.print()
        print(Color.wrap(Color.CYAN, f"⚡ [DSA IN ACTION: Prefix Trie lookup executed in {elapsed:.3f} ms (O(P + K) complexity)]"))

    def handle_bst_range(self):
        print(Color.wrap(Color.BOLD, "\n--- BINARY SEARCH TREE RANGE QUERY ---"))
        while True:
            try:
                min_val = float(input("Minimum Balance ($): ").strip())
                max_val = float(input("Maximum Balance ($): ").strip())
                Validation.validate_balance_range(min_val, max_val)
                break
            except ValueError as e:
                print(Color.wrap(Color.RED, f"  ✖ [RANGE ERROR] {e}"))

        import time
        t0 = time.perf_counter()
        matches = self.service.balance_bst.find_range(min_val, max_val)
        elapsed = (time.perf_counter() - t0) * 1000
        table = TablePrinter("Account Number", "Account Holder", "Type", "Balance", "Status").set_title(f"Accounts in Range [${min_val:.2f} - ${max_val:.2f}]").set_right_align(3)
        for a in matches:
            table.add_row(a.acc_num, a.cust_name, a.acc_type, f"${a.balance:.2f}", "ACTIVE" if a.active else "FROZEN")
        table.print()
        print(Color.wrap(Color.CYAN, f"⚡ [DSA IN ACTION: BST range search executed in {elapsed:.3f} ms (O(log N + K) complexity)]"))

    def handle_priority_desk(self):
        print(Color.wrap(Color.BOLD, "\n--- VIP PRIORITY CUSTOMER DESK (BINARY MAX-HEAP) ---"))
        print("1) Submit Support Ticket\n2) Dispatch Next Priority Ticket (extractMax)\n3) View Current Queue")
        sub = input("Select Option (1/2/3): ").strip()
        if sub == "1":
            cid = input("Customer ID (e.g., CUST-1001): ").strip()
            c = self.service.customers.get(cid)
            if not c:
                print(Color.wrap(Color.RED, f"  ✖ Customer ID {cid} not found."))
                return
            q = input("Issue / Query: ").strip()
            if not q or len(q) < 3:
                print(Color.wrap(Color.RED, "  ✖ Query details must be at least 3 characters long."))
                return
            req = ServiceRequest(c.cust_id, c.name, c.tier, q)
            self.service.priority_desk.insert(req)
            print(Color.wrap(Color.GREEN, f"✔ Ticket submitted: {req.req_id} [Tier: {c.tier[1]}]"))
        elif sub == "2":
            req = self.service.priority_desk.extract_max()
            if req:
                print(Color.wrap(Color.BRIGHT_CYAN, "\n>>> DISPATCHING HIGHEST PRIORITY TICKET <<<"))
                print(f"  Ticket ID: {req.req_id} | Customer: {req.cust_name} ({req.cust_id}) | Priority: {req.tier[1]}")
                print(f"  Query: {req.query}")
                print(Color.wrap(Color.DIM, "  [DSA IN ACTION: Max-Heap root extracted in O(log N) with siftDown]"))
            else:
                print(Color.wrap(Color.YELLOW, "Queue is empty."))
        elif sub == "3":
            items = self.service.priority_desk.to_sorted_list()
            table = TablePrinter("Rank", "Ticket ID", "Tier / Priority", "Customer", "Query Details").set_title("Priority Queue (Sorted by Heap Priority)")
            for i, r in enumerate(items, 1):
                table.add_row(str(i), r.req_id, r.tier[1], r.cust_name, r.query)
            table.print()

    def handle_aml_fraud(self):
        print(Color.wrap(Color.BOLD, "\n--- FORENSIC GRAPH ANALYSIS & ANTI-MONEY LAUNDERING (AML) ---"))
        print("1) Trace Shortest Transfer Path (BFS)\n2) Detect Circular Laundering Loops (DFS)")
        sub = input("Select Option (1/2): ").strip()
        if sub == "1":
            f = self.prompt_validated_account_number("Source Account: ")
            t = self.prompt_validated_account_number("Target Account: ")
            path = self.service.money_graph.shortest_path_bfs(f, t)
            if path:
                print(Color.wrap(Color.GREEN, "\n✔ Shortest Transfer Path Found (BFS):"))
                print(f"  {Color.wrap(Color.BOLD, ' ──► '.join(path))}")
                print(f"  Total transfer hops: {len(path) - 1}")
                print(Color.wrap(Color.DIM, "  [DSA IN ACTION: BFS discovered shortest transfer path in O(V + E)]"))
            else:
                print(Color.wrap(Color.YELLOW, f"No transfer connection found between {f} and {t}."))
        elif sub == "2":
            acc = self.prompt_validated_account_number("Account to Audit for Circular Flows: ")
            cycles = self.service.money_graph.detect_cycles_dfs(acc)
            if cycles:
                print(Color.wrap(Color.RED, f"\n⚠ WARNING: {len(cycles)} CIRCULAR FLOW CYCLE(S) DETECTED!"))
                for i, c in enumerate(cycles, 1):
                    print(f"  Cycle #{i}: {Color.wrap(Color.BRIGHT_RED, ' ──► '.join(c))}")
                print(Color.wrap(Color.DIM, "  [DSA IN ACTION: DFS with Backtracking detected circular ring in O(V + E)]"))
            else:
                print(Color.wrap(Color.GREEN, f"✔ No circular money flows detected for {acc}."))

    def handle_diagnostics(self):
        print(Color.wrap(Color.BOLD, "\n--- SYSTEM DIAGNOSTICS & LRU CACHE TELEMETRY ---"))
        print(Color.wrap(Color.BRIGHT_CYAN, self.service.account_cache.get_summary()))
        table = TablePrinter("Account Number", "Holder", "Type", "Balance", "Status").set_title(f"Accounts Inventory ({len(self.service.accounts)} registered)").set_right_align(3)
        for a in self.service.accounts.values():
            table.add_row(a.acc_num, a.cust_name, a.acc_type, f"${a.balance:.2f}", "ACTIVE" if a.active else "CLOSED")
        table.print()

    def handle_demo_data(self):
        print(Color.wrap(Color.BOLD, "\n--- SEED REALISTIC DEMO DATASET ---"))
        self.service.seed_demo_data()
        print(Color.wrap(Color.GREEN, "✔ Successfully loaded 6 customers, 6 accounts, multiple transfers, and VIP tickets!"))
        print("  * Accounts generated: AC-100001 through AC-100006")
        print("  * Circular AML ring created: AC-100001 -> AC-100002 -> AC-100003 -> AC-100001")

if __name__ == "__main__":
    app = ConsoleBankApp()
    app.run()
