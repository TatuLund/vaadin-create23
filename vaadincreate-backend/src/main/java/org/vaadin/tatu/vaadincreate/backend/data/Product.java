package org.vaadin.tatu.vaadincreate.backend.data;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.vaadin.tatu.vaadincreate.backend.ProductDataService;

@SuppressWarnings("serial")
public class Product extends AbstractEntity {

    @NotNull(message = "{product.name.required}")
    @Size(min = 2, max = 100, message = "{product.name.min2max200}")
    private String productName = "";
    @Min(value = 0, message = "{price.not.negative}")
    private BigDecimal price = BigDecimal.ZERO;
    private Set<Integer> category = Collections.emptySet();
    @Min(value = 0, message = "{stock.not.negative}")
    @NotNull(message = "{stock.required}")
    private Integer stockCount = 0;
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
        return ProductDataService.get().findCategoriesByIds(category);
    }

    public void setCategory(Set<Category> category) {
        this.category = category.stream()
                .map(cat -> Integer.valueOf(cat.getId()))
                .collect(Collectors.toSet());
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
