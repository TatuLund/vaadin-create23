package org.vaadin.tatu.vaadincreate.backend.data;

import java.math.BigDecimal;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import org.jspecify.annotations.NullMarked;

/**
 * Entity representing a line item in a purchase request. Each line references a
 * product with a snapshot of its price at request time.
 */
@NullMarked
@SuppressWarnings({ "serial", "java:S2160" })
@Entity
@Table(name = "purchase_line")
public class PurchaseLine extends AbstractEntity {

    @NotNull(message = "{purchase.required}")
    @ManyToOne(optional = false)
    @JoinColumn(name = "purchase_id", nullable = false)
    private Purchase purchase;

    @NotNull(message = "{product.required}")
    @ManyToOne(optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Min(value = 1, message = "{quantity.min}")
    @NotNull(message = "{quantity.required}")
    @Column(name = "quantity", nullable = false)
    private Integer quantity = 1;

    @NotNull(message = "{unitPrice.required}")
    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    @SuppressWarnings("null")
    private BigDecimal unitPrice = BigDecimal.ZERO;

    /**
     * Default constructor.
     */
    public PurchaseLine() {
    }

    /**
     * Constructs a PurchaseLine with all required fields.
     *
     * @param purchase
     *            the parent purchase
     * @param product
     *            the product being purchased
     * @param quantity
     *            the quantity requested
     * @param unitPrice
     *            the unit price snapshot at request time
     */
    public PurchaseLine(Purchase purchase, Product product, Integer quantity,
            BigDecimal unitPrice) {
        this.purchase = Objects.requireNonNull(purchase,
                "Purchase must not be null");
        this.product = Objects.requireNonNull(product,
                "Product must not be null");
        this.quantity = Objects.requireNonNull(quantity,
                "Quantity must not be null");
        this.unitPrice = Objects.requireNonNull(unitPrice,
                "Unit price must not be null");
    }

    public Purchase getPurchase() {
        return purchase;
    }

    public void setPurchase(Purchase purchase) {
        this.purchase = purchase;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    /**
     * Calculates the total price for this line item. This is a derived value
     * (unitPrice * quantity).
     *
     * @return the line total
     */
    @Transient
    public BigDecimal getLineTotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
