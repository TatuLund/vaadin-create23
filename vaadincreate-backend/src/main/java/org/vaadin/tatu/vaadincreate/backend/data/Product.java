package org.vaadin.tatu.vaadincreate.backend.data;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.jspecify.annotations.NullMarked;

@NullMarked
@SuppressWarnings({ "serial", "java:S2160" })
@Entity
public class Product extends AbstractEntity {

    @NotNull(message = "{product.name.required}")
    @Size(min = 2, max = 100, message = "{product.name.min2max200}")
    @Column(name = "product_name")
    private String productName = "";

    @Min(value = 0, message = "{price.not.negative}")
    @Column(name = "price")
    private BigDecimal price = BigDecimal.ZERO;

    // Using Eager as the category is shown in the Grid, Lazy would not help
    // performance.

    @ManyToMany(fetch = FetchType.EAGER, cascade = { CascadeType.PERSIST,
            CascadeType.MERGE, CascadeType.DETACH })
    @JoinTable(name = "product_category", joinColumns = @JoinColumn(name = "product_id"), inverseJoinColumns = @JoinColumn(name = "category_id"))
    private Set<Category> category = Collections.emptySet();

    @Min(value = 0, message = "{stock.not.negative}")
    @NotNull(message = "{stock.required}")
    @Column(name = "stock_count")
    private Integer stockCount = 0;

    @NotNull(message = "{availability.required}")
    @Column(name = "availability")
    @Enumerated(EnumType.STRING)
    private Availability availability = Availability.COMING;

    public Product() {
    }

    /**
     * Copy constructor for creating a new Product instance by copying the
     * properties of another Product instance.
     *
     * @param other
     *            the Product instance to copy from
     * @throws NullPointerException
     *             if the provided Product instance is null
     */
    public Product(Product other) {
        setId(other.getId());
        setProductName(other.getProductName());
        setPrice(other.getPrice());
        setStockCount(other.getStockCount());
        setAvailability(other.getAvailability());
        setCategory(other.getCategory());
        version = other.version;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Set<Category> getCategory() {
        return category;
    }

    public void setCategory(Set<Category> category) {
        this.category = category;
    }

    public Integer getStockCount() {
        return stockCount;
    }

    public void setStockCount(Integer stockCount) {
        this.stockCount = stockCount;
    }

    public Availability getAvailability() {
        return availability;
    }

    public void setAvailability(Availability availability) {
        this.availability = availability;
    }
}
