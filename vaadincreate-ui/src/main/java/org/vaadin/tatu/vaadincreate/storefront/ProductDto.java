package org.vaadin.tatu.vaadincreate.storefront;

import java.io.Serializable;
import java.math.BigDecimal;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Data transfer object for product selection in purchase wizard. Combines
 * product information with order quantity.
 */
@NullMarked
@SuppressWarnings("serial")
public class ProductDto implements Serializable {
    private final Integer productId;
    private final String productName;
    private final Integer stockCount;
    private final BigDecimal price;
    private Integer orderQuantity;

    public ProductDto(Integer productId, String productName, Integer stockCount,
            BigDecimal price) {
        this.productId = productId;
        this.productName = productName;
        this.stockCount = stockCount;
        this.price = price;
        this.orderQuantity = 0;
    }

    public Integer getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public Integer getStockCount() {
        return stockCount;
    }

    public BigDecimal getPrice() {
        return price;
    }

    @Nullable
    public Integer getOrderQuantity() {
        return orderQuantity;
    }

    public void setOrderQuantity(@Nullable Integer orderQuantity) {
        this.orderQuantity = orderQuantity == null ? 0 : orderQuantity;
    }

    public BigDecimal getLineTotal() {
        if (orderQuantity == null || orderQuantity == 0) {
            return BigDecimal.ZERO;
        }
        return price.multiply(BigDecimal.valueOf(orderQuantity));
    }
}
