package org.vaadin.tatu.vaadincreate.backend.service;

import java.util.Collection;
import java.util.Objects;
import java.util.Random;
import java.util.Set;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.tatu.vaadincreate.backend.ProductDataService;
import org.vaadin.tatu.vaadincreate.backend.dao.DraftDao;
import org.vaadin.tatu.vaadincreate.backend.dao.ProductDao;
import org.vaadin.tatu.vaadincreate.backend.data.Category;
import org.vaadin.tatu.vaadincreate.backend.data.Draft;
import org.vaadin.tatu.vaadincreate.backend.data.Product;
import org.vaadin.tatu.vaadincreate.backend.data.User;
import org.vaadin.tatu.vaadincreate.backend.mock.MockDataGenerator;

@NullMarked
@SuppressWarnings("java:S6548")
public class ProductDataServiceImpl implements ProductDataService {
    private static final String ID_CANT_BE_NULL = "id can't be null";

    // Service class for managing products
    // This class is a singleton
    private static ProductDataServiceImpl instance;
    private ProductDao productDao = new ProductDao();
    private DraftDao draftDao = new DraftDao();
    private Random random = new Random();
    private boolean slow = false;

    private ProductDataServiceImpl() {
        var backendMode = System.getProperty("backend.mode");
        if (backendMode != null && backendMode.equals("slow")) {
            slow = true;
        }
        var env = System.getProperty("generate.data");
        if (env == null || env.equals("true")) {
            var categories = MockDataGenerator.createCategories();
            categories.forEach(cat -> productDao.updateCategory(cat));
            var savedCategories = productDao.getAllCategories().stream()
                    .toList();
            var products = MockDataGenerator.createProducts(savedCategories);
            products.forEach(prod -> productDao.updateProduct(prod));
            logger.info("Generated mock product data");
        }
    }

    public static synchronized ProductDataService getInstance() {
        if (instance == null) {
            instance = new ProductDataServiceImpl();
        }
        return instance;
    }

    @Override
    public synchronized Product updateProduct(Product product) {
        Objects.requireNonNull(product, "product can't be null");
        randomWait(1);
        return productDao.updateProduct(product);
    }

    @Override
    public synchronized void deleteProduct(Integer id) {
        Objects.requireNonNull(id, ID_CANT_BE_NULL);
        randomWait(1);
        productDao.deleteProduct(id);
    }

    @Nullable
    @Override
    public synchronized Product getProductById(Integer id) {
        Objects.requireNonNull(id, ID_CANT_BE_NULL);
        randomWait(1);
        return productDao.getProduct(id);
    }

    @Override
    public synchronized Collection<Product> getAllProducts() {
        randomWait(6);
        return productDao.getAllProducts();
    }

    @Override
    public synchronized Collection<Category> getAllCategories() {
        randomWait(2);
        return productDao.getAllCategories();
    }

    @Override
    public synchronized void deleteCategory(Integer id) {
        Objects.requireNonNull(id, ID_CANT_BE_NULL);
        randomWait(2);
        var category = productDao.getCategory(id);
        if (category == null) {
            throw new IllegalArgumentException("Category not found");
        }

        productDao.deleteCategory(id);
    }

    @Override
    public synchronized Set<Category> findCategoriesByIds(Set<Integer> ids) {
        Objects.requireNonNull(ids, "ids can't be null");
        randomWait(1);
        return productDao.getCategoriesByIds(ids);
    }

    @Override
    public synchronized Category updateCategory(Category category) {
        Objects.requireNonNull(category, "category can't be null");
        randomWait(1);
        if (category.getId() == null
                && productDao.getCategoryByName(category.getName()) != null) {
            throw new IllegalArgumentException(
                    "Category with the same name already exists");
        }
        return productDao.updateCategory(category);
    }

    @Override
    public void saveDraft(User user, @Nullable Product draftProduct) {
        Objects.requireNonNull(user, "user can't be null");
        randomWait(1);
        logger.info("Saving draft for user '{}'", user.getName());
        if (draftProduct == null) {
            draftDao.deleteDraft(user);
        } else {
            var draft = new Draft(draftProduct, user);
            draftDao.updateDraft(draft);
        }
    }

    @Nullable
    @Override
    public Product findDraft(User user) {
        Objects.requireNonNull(user, "user can't be null");
        logger.info("Finding draft for user '{}'", user.getName());
        var draft = draftDao.findDraft(user);
        return draft != null ? draft.toProduct() : null;
    }

    @SuppressWarnings("java:S2142")
    private void randomWait(int count) {
        if (!slow) {
            return;
        }
        int wait = 20 + random.nextInt(40);
        try {
            Thread.sleep(wait * (long) count);
        } catch (InterruptedException e) {
            // NOP
        }
    }

    private Logger logger = LoggerFactory.getLogger(this.getClass());

}
