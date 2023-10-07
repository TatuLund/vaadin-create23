package org.vaadin.tatu.vaadincreate.backend.mock;

import java.util.List;

import org.vaadin.tatu.vaadincreate.backend.ProductDataService;
import org.vaadin.tatu.vaadincreate.backend.data.Category;
import org.vaadin.tatu.vaadincreate.backend.data.Product;

/**
 * Mock data model. This implementation has very simplistic locking and does not
 * notify users of modifications. There are mocked delays to simulate real
 * database response times.
 */
@SuppressWarnings("serial")
public class MockProductDataService extends ProductDataService {

    private static MockProductDataService INSTANCE;

    private List<Product> products;
    private List<Category> categories;
    private int nextProductId = 0;

    private MockProductDataService() {
        categories = MockDataGenerator.createCategories();
        products = MockDataGenerator.createProducts(categories);
        nextProductId = products.size() + 1;
    }

    public synchronized static ProductDataService getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new MockProductDataService();
        }
        return INSTANCE;
    }

    @Override
    public synchronized List<Product> getAllProducts() {
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
        }
        return products;
    }

    @Override
    public synchronized List<Category> getAllCategories() {
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
        }
        return categories;
    }

    @Override
    public synchronized Product updateProduct(Product p) {
        try {
            Thread.sleep(250);
        } catch (InterruptedException e) {
        }
        if (p.getId() < 0) {
            // New product
            p.setId(nextProductId++);
            products.add(p);
            return p;
        }
        for (int i = 0; i < products.size(); i++) {
            if (products.get(i).getId() == p.getId()) {
                products.set(i, p);
                return p;
            }
        }

        throw new IllegalArgumentException(
                "No product with id " + p.getId() + " found");
    }

    @Override
    public synchronized Product getProductById(int productId) {
        try {
            Thread.sleep(250);
        } catch (InterruptedException e) {
        }
        for (int i = 0; i < products.size(); i++) {
            if (products.get(i).getId() == productId) {
                return products.get(i);
            }
        }
        return null;
    }

    @Override
    public synchronized void deleteProduct(int productId) {
        try {
            Thread.sleep(250);
        } catch (InterruptedException e) {
        }
        Product p = getProductById(productId);
        if (p == null) {
            throw new IllegalArgumentException(
                    "Product with id " + productId + " not found");
        }
        products.remove(p);
    }
}
