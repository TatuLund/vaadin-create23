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
import javax.persistence.OneToOne;

@SuppressWarnings({ "serial", "java:S2160" })
@Entity
public class Draft extends AbstractEntity {

    @Column(name = "product_id")
    private Integer productId;

    @OneToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "product_name")
    private String productName = "";

    @Column(name = "price")
    private BigDecimal price = BigDecimal.ZERO;

    @ManyToMany(fetch = FetchType.EAGER, cascade = { CascadeType.PERSIST,
            CascadeType.MERGE, CascadeType.DETACH })
    @JoinTable(name = "draft_category", joinColumns = @JoinColumn(name = "draft_id"), inverseJoinColumns = @JoinColumn(name = "category_id"))
    private Set<Category> category = Collections.emptySet();

    @Column(name = "stock_count")
    private Integer stockCount = 0;

    @Enumerated(EnumType.STRING)
    private Availability availability = Availability.COMING;

    public Draft() {
    }

    /**
     * Constructs a new Draft object with the specified product and user.
     *
     * @param product
     *            the product associated with the draft
     * @param user
     *            the user associated with the draft
     */
    public Draft(Product product, User user) {
        setProductId(product.getId());
        setProductName(product.getProductName());
        setPrice(product.getPrice());
        setCategory(product.getCategory());
        setStockCount(product.getStockCount());
        setAvailability(product.getAvailability());
        setUser(user);
    }

    /**
     * Converts the current Draft instance to a Product instance.
     *
     * @return a Product instance with properties copied from the current Draft
     *         instance.
     */
    public Product toProduct() {
        Product product = new Product();
        product.setId(getProductId());
        product.setProductName(getProductName());
        product.setPrice(getPrice());
        product.setCategory(getCategory());
        product.setStockCount(getStockCount());
        product.setAvailability(getAvailability());
        return product;
    }

    private Integer getProductId() {
        return productId;
    }

    private void setProductId(Integer productId) {
        this.productId = productId;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
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
