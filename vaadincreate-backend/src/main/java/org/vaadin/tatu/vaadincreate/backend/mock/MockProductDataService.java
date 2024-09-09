package org.vaadin.tatu.vaadincreate.backend.mock;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
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
@SuppressWarnings({ "serial", "java:S6548" })
public class MockProductDataService extends ProductDataService {

    private static MockProductDataService instance;

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

    public static synchronized ProductDataService getInstance() {
        if (instance == null) {
            instance = new MockProductDataService();
        }
        return instance;
    }

    @Override
    public synchronized List<Product> getAllProducts() {
        synchronized (products) {
            randomWait(6);
            return products.stream().map(Product::new)
                    .collect(Collectors.toList());
        }
    }

    @Override
    public List<Category> getAllCategories() {
        synchronized (categories) {
            randomWait(2);
            return categories.stream().map(Category::new)
                    .collect(Collectors.toList());
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
                    p.setVersion(products.get(i).getVersion() + 1);
                    products.set(i, p);
                    logger.info("Updated the product ({}) {}", p.getId(),
                            p.getProductName());
                    return p;
                }
            }

            throw new IllegalArgumentException(
                    String.format("No product with id %d found", p.getId()));
        }
    }

    @Override
    public Product getProductById(int productId) {
        synchronized (products) {
            randomWait(1);
            for (Product product : products) {
                if (product.getId() == productId) {
                    return new Product(product);
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
                throw new IllegalArgumentException(String
                        .format("Product with id %d not found", productId));
            }
            products.remove(p);
        }
    }

    @Override
    public Category updateCategory(Category category) {
        Objects.requireNonNull(category);
        synchronized (categories) {
            randomWait(1);
            throwIfInvalidCategory(category);
            var newCategory = new Category(category);
            if (newCategory.getId() < 0) {
                newCategory.setId(nextCategoryId++);
                categories.add(newCategory);
                logger.info("Category {} created", newCategory.getId());
            } else {
                var index = categories.indexOf(newCategory);
                if (index < 0) {
                    throw new IllegalArgumentException(
                            String.format("Category with id %d does not exist.",
                                    newCategory.getId()));
                }
                newCategory.setVersion(categories.get(index).getVersion() + 1);
                categories.set(index, newCategory);
                logger.info("Category {} updated", newCategory.getId());
            }
            return newCategory;
        }
    }

    private void throwIfInvalidCategory(Category category) {
        Optional<Category> old;
        old = categories.stream()
                .filter(item -> item.getName().equals(category.getName()))
                .findFirst();
        old.ifPresent(oldCategory -> {
            if (category.getId() < 0 || (category.getId() >= 0
                    && oldCategory.getId() != category.getId())) {
                throw new IllegalStateException(String.format(
                        "Cannot re-use category name: %s", category.getName()));
            }
        });
    }

    @Override
    public void deleteCategory(int categoryId) {
        synchronized (categories) {
            randomWait(1);
            if (getAllProducts().stream()
                    .noneMatch(c -> c.getId() == categoryId)) {
                throw new IllegalArgumentException(String
                        .format("Category with id %d not found", categoryId));
            }
            deleteCategoryInternal(categoryId);
        }
        logger.info("Category {} deleted", categoryId);
    }

    private void deleteCategoryInternal(int categoryId) {
        if (categories.removeIf(category -> category.getId() == categoryId)) {
            getAllProducts().forEach(product -> product.getCategory()
                    .removeIf(category -> category.getId() == categoryId));
        }
    }

    @Override
    public Set<Category> findCategoriesByIds(Set<Integer> categoryIds) {
        synchronized (categories) {
            return categories.stream()
                    .filter(cat -> categoryIds.contains(cat.getId()))
                    .map(Category::new).collect(Collectors.toSet());
        }
    }

    @Override
    public Collection<Product> backup() {
        return products.stream().map(Product::new).collect(Collectors.toList());
    }

    @Override
    public void restore(Collection<Product> data) {
        products.clear();
        data.forEach(product -> products.add(new Product(product)));
    }

    @SuppressWarnings("java:S2142")
    private void randomWait(int count) {
        int wait = 50 + random.nextInt(100);
        try {
            Thread.sleep(wait * (long) count);
        } catch (InterruptedException e) {
            // NOP
        }
    }

    private Logger logger = LoggerFactory.getLogger(this.getClass());
}
