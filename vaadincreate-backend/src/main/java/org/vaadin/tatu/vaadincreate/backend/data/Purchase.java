package org.vaadin.tatu.vaadincreate.backend.data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Entity representing a purchase request in the system.
 * A purchase contains multiple lines of products, delivery information,
 * and approval workflow state.
 */
@NullMarked
@SuppressWarnings({ "serial", "java:S2160" })
@Entity
@Table(name = "purchase")
public class Purchase extends AbstractEntity {

    @NotNull(message = "{requester.required}")
    @ManyToOne(optional = false)
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;

    @Nullable
    @ManyToOne
    @JoinColumn(name = "approver_id")
    private User approver;

    @NotNull(message = "{status.required}")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PurchaseStatus status = PurchaseStatus.PENDING;

    @NotNull(message = "{createdAt.required}")
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Nullable
    @Column(name = "decided_at")
    private Instant decidedAt;

    @Nullable
    @Column(name = "decision_reason", length = 500)
    private String decisionReason;

    @NotNull(message = "{deliveryAddress.required}")
    @Embedded
    private Address deliveryAddress;

    @OneToMany(mappedBy = "purchase", cascade = CascadeType.ALL, orphanRemoval = true)
    @SuppressWarnings("null")
    private List<PurchaseLine> lines = new ArrayList<>();

    /**
     * Default constructor.
     */
    public Purchase() {
        this.createdAt = Instant.now();
        this.deliveryAddress = new Address();
    }

    /**
     * Constructs a Purchase with required fields.
     *
     * @param requester the user making the purchase request
     * @param deliveryAddress the delivery address for this purchase
     */
    public Purchase(User requester, Address deliveryAddress) {
        this.requester = Objects.requireNonNull(requester, "Requester must not be null");
        this.deliveryAddress = Objects.requireNonNull(deliveryAddress, "Delivery address must not be null");
        this.createdAt = Instant.now();
        this.status = PurchaseStatus.PENDING;
    }

    public User getRequester() {
        return requester;
    }

    public void setRequester(User requester) {
        this.requester = requester;
    }

    @Nullable
    public User getApprover() {
        return approver;
    }

    public void setApprover(@Nullable User approver) {
        this.approver = approver;
    }

    public PurchaseStatus getStatus() {
        return status;
    }

    public void setStatus(PurchaseStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    @Nullable
    public Instant getDecidedAt() {
        return decidedAt;
    }

    public void setDecidedAt(@Nullable Instant decidedAt) {
        this.decidedAt = decidedAt;
    }

    @Nullable
    public String getDecisionReason() {
        return decisionReason;
    }

    public void setDecisionReason(@Nullable String decisionReason) {
        this.decisionReason = decisionReason;
    }

    public Address getDeliveryAddress() {
        return deliveryAddress;
    }

    public void setDeliveryAddress(Address deliveryAddress) {
        this.deliveryAddress = deliveryAddress;
    }

    public List<PurchaseLine> getLines() {
        return lines;
    }

    public void setLines(List<PurchaseLine> lines) {
        this.lines = lines;
    }

    /**
     * Adds a line item to this purchase.
     * This is a convenience method that maintains the bidirectional relationship.
     *
     * @param line the line to add
     */
    public void addLine(PurchaseLine line) {
        Objects.requireNonNull(line, "Line must not be null");
        lines.add(line);
        line.setPurchase(this);
    }

    /**
     * Removes a line item from this purchase.
     *
     * @param line the line to remove
     */
    public void removeLine(PurchaseLine line) {
        Objects.requireNonNull(line, "Line must not be null");
        lines.remove(line);
        line.setPurchase(null);
    }

    /**
     * Calculates the total amount for this purchase.
     * This is a derived value summing all line totals.
     *
     * @return the total purchase amount
     */
    @Transient
    public BigDecimal getTotalAmount() {
        return lines.stream()
                .map(PurchaseLine::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
