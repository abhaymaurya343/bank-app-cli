package com.bank.domain;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Customer domain model.
 */
public class Customer {
    private final String customerId;
    private String name;
    private String email;
    private String phone;
    private CustomerTier tier;
    private final LocalDateTime registeredAt;

    public Customer(String customerId, String name, String email, String phone, CustomerTier tier) {
        this.customerId = customerId;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.tier = tier != null ? tier : CustomerTier.REGULAR;
        this.registeredAt = LocalDateTime.now();
    }

    public Customer(String customerId, String name, String email) {
        this(customerId, name, email, "N/A", CustomerTier.REGULAR);
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public CustomerTier getTier() {
        return tier;
    }

    public void setTier(CustomerTier tier) {
        this.tier = tier;
    }

    public LocalDateTime getRegisteredAt() {
        return registeredAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Customer customer = (Customer) o;
        return Objects.equals(customerId, customer.customerId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(customerId);
    }

    @Override
    public String toString() {
        return String.format("Customer[id=%s, name='%s', email='%s', tier=%s]",
                customerId, name, email, tier);
    }
}
