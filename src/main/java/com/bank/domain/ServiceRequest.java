package com.bank.domain;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents a customer support / service ticket submitted to the Priority Queue.
 * Priority is determined by Customer Tier and submission order (Max-Heap comparable).
 */
public class ServiceRequest implements Comparable<ServiceRequest> {
    private final String requestId;
    private final String customerId;
    private final String customerName;
    private final CustomerTier tier;
    private final String queryDetails;
    private final LocalDateTime submittedAt;

    public ServiceRequest(String customerId, String customerName, CustomerTier tier, String queryDetails) {
        this.requestId = "REQ-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        this.customerId = customerId;
        this.customerName = customerName;
        this.tier = tier != null ? tier : CustomerTier.REGULAR;
        this.queryDetails = queryDetails;
        this.submittedAt = LocalDateTime.now();
    }

    public String getRequestId() {
        return requestId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public CustomerTier getTier() {
        return tier;
    }

    public String getQueryDetails() {
        return queryDetails;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public String getFormattedTimestamp() {
        return submittedAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    /**
     * Max-Heap ordering:
     * 1. Higher tier weight comes first (PLATINUM(4) > GOLD(3) > SILVER(2) > REGULAR(1)).
     * 2. If same tier, older requests (FIFO tie-breaker) have priority.
     */
    @Override
    public int compareTo(ServiceRequest other) {
        if (other == null) return 1;
        int tierCompare = Integer.compare(this.tier.getPriorityWeight(), other.tier.getPriorityWeight());
        if (tierCompare != 0) {
            return tierCompare; // higher priority weight wins
        }
        // Earlier submission time has higher priority (reverse timestamp comparison)
        return other.submittedAt.compareTo(this.submittedAt);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ServiceRequest that = (ServiceRequest) o;
        return Objects.equals(requestId, that.requestId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(requestId);
    }

    @Override
    public String toString() {
        return String.format("[%s] Tier: %-12s | Customer: %-15s | Query: %s",
                requestId, tier.getLabel(), customerName, queryDetails);
    }
}
