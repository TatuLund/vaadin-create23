package org.vaadin.tatu.vaadincreate.backend;

import java.io.Serializable;
import java.util.Collection;

import org.vaadin.tatu.vaadincreate.backend.data.Category;
import org.vaadin.tatu.vaadincreate.backend.data.Product;
import org.vaadin.tatu.vaadincreate.backend.mock.MockProductDataService;

/**
 * Back-end service interface for retrieving and updating product data.
 */
@SuppressWarnings("serial")
public abstract class ProductDataService implements Serializable {

    /**
     * Get all Products in the database.
     * 
     * @return Collection of Product
     */
    public abstract Collection<Product> getAllProducts();

    /**
     * Get all Categories in the database.
     * 
     * @return Collection of Category
     */
    public abstract Collection<Category> getAllCategories();

    /**
     * Updates or saves a new Product. If {@link Product#getId()} is -1 product
     * is being assigned actual id and saved as new.
     * 
     * @param p
     *            Product to be updated/saved
     * @return Saved product instance
     * @throws IllegalArgumentException
     *             if product did not exists
     */
    public abstract Product updateProduct(Product p);

    /**
     * Deelete the product by id
     * 
     * @param productId
     *            id of the Product to be deleted
     * @throws IllegalArgumentException
     *             if product did not exists
     */
    public abstract void deleteProduct(int productId);

    /**
     * Find a Product from database using id
     * 
     * @param productId
     *            id of the Product
     * @return Product if it was found, otherwise null
     */
    public abstract Product getProductById(int productId);

    public static ProductDataService get() {
        return MockProductDataService.getInstance();
    }

}
