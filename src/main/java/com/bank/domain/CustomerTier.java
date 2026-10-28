package com.bank.domain;

/**
 * Customer tiers with associated priority levels for request queues.
 * Higher priority rank means higher precedence in service dispatch.
 */
public enum CustomerTier {
    REGULAR("Regular", 1),
    SILVER("Silver", 2),
    GOLD("Gold", 3),
    PLATINUM("Platinum VIP", 4);

    private final String label;
    private final int priorityWeight;

    CustomerTier(String label, int priorityWeight) {
        this.label = label;
        this.priorityWeight = priorityWeight;
    }

    public String getLabel() {
        return label;
    }

    public int getPriorityWeight() {
        return priorityWeight;
    }
}
