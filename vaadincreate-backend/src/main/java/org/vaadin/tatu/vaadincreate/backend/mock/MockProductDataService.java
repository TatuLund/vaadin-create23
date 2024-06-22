package org.vaadin.tatu.vaadincreate.backend.mock;

import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private int nextCategoryId = 0;

    Random random = new Random();

    private MockProductDataService() {
        categories = MockDataGenerator.createCategories();
        products = MockDataGenerator.createProducts(categories);
        nextProductId = products.size() + 1;
        nextCategoryId = categories.size() + 1;
        logger.info("Generated mock product data");
    }

    public synchronized static ProductDataService getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new MockProductDataService();
        }
        return INSTANCE;
    }

    @Override
    public synchronized List<Product> getAllProducts() {
        synchronized (products) {
            randomWait(12);
            return products.stream().map(p -> new Product(p))
                    .collect(Collectors.toList());
        }
    }

    @Override
    public List<Category> getAllCategories() {
        synchronized (categories) {
            randomWait(2);
            return categories;
        }
    }

    @Override
    public Product updateProduct(Product product) {
        synchronized (products) {
            randomWait(1);
            var p = new Product(product);
            if (p.getId() < 0) {
                // New product
                p.setId(nextProductId++);
                products.add(p);
                logger.info("Saved a new product ({}) {}", p.getId(),
                        p.getProductName());
                return p;
            }
            for (int i = 0; i < products.size(); i++) {
                if (products.get(i).getId() == p.getId()) {
                    products.set(i, p);
                    logger.info("Updated the product ({}) {}", p.getId(),
                            p.getProductName());
                    return p;
                }
            }

            throw new IllegalArgumentException(
                    "No product with id " + p.getId() + " found");
        }
    }

    @Override
    public Product getProductById(int productId) {
        synchronized (products) {
            randomWait(1);
            for (int i = 0; i < products.size(); i++) {
                if (products.get(i).getId() == productId) {
                    return new Product(products.get(i));
                }
            }
            return null;
        }
    }

    @Override
    public void deleteProduct(int productId) {
        synchronized (products) {
            randomWait(1);
            Product p = getProductById(productId);
            if (p == null) {
                throw new IllegalArgumentException(
                        "Product with id " + productId + " not found");
            }
            products.remove(p);
        }
    }

    @Override
    public Category updateCategory(Category category) {
        synchronized (categories) {
            randomWait(1);
            var newCategory = new Category(category);
            if (newCategory.getId() < 0) {
                newCategory.setId(nextCategoryId++);
                categories.add(newCategory);
                logger.info("Category {} created", newCategory.getId());
            } else {
                deleteCategoryInternal(category.getId());
                categories.add(newCategory);
                logger.info("Category {} updated", newCategory.getId());
            }
            return newCategory;
        }
    }

    @Override
    public void deleteCategory(int categoryId) {
        synchronized (categories) {
            randomWait(1);
            if (!getAllProducts().stream()
                    .anyMatch(c -> c.getId() == categoryId)) {
                throw new IllegalArgumentException(
                        "Category with id " + categoryId + " not found");
            }
            deleteCategoryInternal(categoryId);
        }
        logger.info("Category {} deleted", categoryId);
    }

    private void deleteCategoryInternal(int categoryId) {
        if (categories.removeIf(category -> category.getId() == categoryId)) {
            getAllProducts().forEach(product -> {
                product.getCategory()
                        .removeIf(category -> category.getId() == categoryId);
            });
        }
    }

    @Override
    public Set<Category> findCategoriesByIds(Set<Integer> categoryIds) {
        synchronized (categories) {
            return categories.stream()
                    .filter(cat -> categoryIds.contains(cat.getId()))
                    .collect(Collectors.toSet());
        }
    }

    @Override
    public Collection<Product> backup() {
        return products.stream().map(product -> new Product(product))
                .collect(Collectors.toList());
    }

    public void restore(Collection<Product> data) {
        products.clear();
        data.forEach(product -> {
            products.add(new Product(product));
        });
    }

    private void randomWait(int count) {
        int wait = 50 + random.nextInt(100);
        try {
            Thread.sleep(wait * count);
        } catch (InterruptedException e) {
        }
    }

    private Logger logger = LoggerFactory.getLogger(this.getClass());
}
