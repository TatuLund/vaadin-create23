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

    public abstract Collection<Product> getAllProducts();

    public abstract Collection<Category> getAllCategories();

    public abstract void updateProduct(Product p);

    public abstract void deleteProduct(int productId);

    public abstract Product getProductById(int productId);

    public static ProductDataService get() {
        return MockProductDataService.getInstance();
    }

}
