package org.vaadin.tatu.vaadincreate.backend.data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.Set;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@SuppressWarnings("serial")
public class Product implements Serializable {

    @NotNull
    @Min(0)
    private int id = -1;
    @NotNull(message = "{product.name.required}")
    @Size(min = 2, max = 100, message = "{product.name.min2max200}")
    private String productName = "";
    @Min(value = 0, message = "{price.not.negative}")
    private BigDecimal price = BigDecimal.ZERO;
    private Set<Category> category;
    @Min(value = 0, message = "{stock.not.negative}")
    private int stockCount = 0;
    @NotNull(message = "{availability.required}")
    private Availability availability = Availability.COMING;

    public Product() {
        setId(-1);
    }

    public Product(Product other) {
        setId(other.getId());
        setProductName(other.getProductName());
        setPrice(other.getPrice());
        setStockCount(other.getStockCount());
        setAvailability(other.getAvailability());
        setCategory(other.getCategory());
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
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

    public int getStockCount() {
        return stockCount;
    }

    public void setStockCount(int stockCount) {
        this.stockCount = stockCount;
    }

    public Availability getAvailability() {
        return availability;
    }

    public void setAvailability(Availability availability) {
        this.availability = availability;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Product other = (Product) obj;
        return id == other.id;
    }
}
