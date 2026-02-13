package org.vaadin.tatu.vaadincreate.backend.data;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.jspecify.annotations.NullMarked;

/**
 * Simple in-memory cart model for collecting products and quantities before
 * creating a purchase request. This is not a persistent entity.
 */
@NullMarked
@SuppressWarnings("serial")
public class Cart implements Serializable {

    private final Map<Product, Integer> items = new HashMap<>();

    /**
     * Adds a product to the cart with the specified quantity.
     *
     * @param product
     *            the product to add
     * @param quantity
     *            the quantity to add
     */
    public void addItem(Product product, int quantity) {
        Objects.requireNonNull(product, "Product must not be null");
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        items.put(product, items.getOrDefault(product, 0) + quantity);
    }

    /**
     * Sets the quantity for a product in the cart.
     *
     * @param product
     *            the product
     * @param quantity
     *            the quantity to set
     */
    public void setQuantity(Product product, int quantity) {
        Objects.requireNonNull(product, "Product must not be null");
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        items.put(product, quantity);
    }

    /**
     * Removes a product from the cart.
     *
     * @param product
     *            the product to remove
     */
    public void removeItem(Product product) {
        Objects.requireNonNull(product, "Product must not be null");
        items.remove(product);
    }

    /**
     * Gets all items in the cart.
     *
     * @return a map of products to quantities
     */
    public Map<Product, Integer> getItems() {
        return new HashMap<>(items);
    }

    /**
     * Clears all items from the cart.
     */
    public void clear() {
        items.clear();
    }

    /**
     * Checks if the cart is empty.
     *
     * @return true if the cart is empty
     */
    public boolean isEmpty() {
        return items.isEmpty();
    }

    /**
     * Gets the number of different products in the cart.
     *
     * @return the number of different products
     */
    public int size() {
        return items.size();
    }
}
