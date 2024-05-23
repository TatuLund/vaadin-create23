package org.vaadin.tatu.vaadincreate.backend;

import java.util.Collection;
import java.util.Set;

import org.vaadin.tatu.vaadincreate.backend.data.Category;
import org.vaadin.tatu.vaadincreate.backend.data.Product;
import org.vaadin.tatu.vaadincreate.backend.mock.MockProductDataService;

/**
 * Back-end service interface for retrieving and updating product data.
 */
public abstract class ProductDataService {

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

    /**
     * Saves a new category or updates an existing one.
     *
     * @param category
     *            the category to save
     * @return the saved category
     */
    public abstract Category updateCategory(Category category);

    /**
     * Deletes a category with the specified category ID.
     *
     * @param categoryId
     *            the ID of the category to delete
     */
    public abstract void deleteCategory(int categoryId);

    /**
     * Find categories by their ids.
     *
     * @param categories
     *            the IDs of the categories to find
     * @return the categories with the given IDs
     */
    public abstract Set<Category> findCategoriesByIds(Set<Integer> categories);

    public abstract Collection<Product> backup();

    public abstract void restore(Collection<Product> data);

    public static ProductDataService get() {
        return MockProductDataService.getInstance();
    }
}
