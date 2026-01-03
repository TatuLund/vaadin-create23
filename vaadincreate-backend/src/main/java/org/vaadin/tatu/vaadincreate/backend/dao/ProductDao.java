package org.vaadin.tatu.vaadincreate.backend.dao;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.tatu.vaadincreate.backend.data.Category;
import org.vaadin.tatu.vaadincreate.backend.data.Product;

/**
 * Data access object for managing products and categories.
 */
@NullMarked
@SuppressWarnings("java:S1602")
public class ProductDao {

    /**
     * Updates an existing product or saves a new product if it does not have an
     * ID.
     * 
     * This method logs the product being persisted and uses a transaction to
     * either update the existing product or save a new product. It then
     * retrieves and returns the persisted product.
     * 
     * @param product
     *            the product to be updated or saved
     * @return the persisted product with the updated information
     */
    public Product updateProduct(Product product) {
        Objects.requireNonNull(product,
                "Product to be updated must not be null");
        Objects.requireNonNull(product.getProductName(),
                "Product name must not be null");
        logger.info("Persisting Product: ({}) '{}'", product.getId(),
                product.getProductName());
        var identifier = HibernateUtil.inTransaction(session -> {
            Integer id;
            if (product.getId() != null) {
                session.update(product);
                id = product.getId();
            } else {
                id = (Integer) session.save(product);
            }
            return id;
        });
        // Necessary: Refetch new version of the product
        var result = HibernateUtil.inSession(session -> {
            @Nullable
            Product prod = session.get(Product.class, identifier);
            return prod;
        });
        if (result == null) {
            throw new IllegalStateException(
                    "Just saved product is now missing: " + identifier);
        }
        return result;
    }

    /**
     * Retrieves a Product by its unique identifier.
     *
     * @param id
     *            the unique identifier of the Product to be fetched
     * @return the Product object corresponding to the given id, or null if no
     *         such Product exists
     */
    @Nullable
    public Product getProduct(Integer id) {
        Objects.requireNonNull(id, "Product ID must not be null");
        logger.info("Fetching Product: ({})", id);
        return HibernateUtil.inSession(session -> {
            @Nullable
            Product prod = session.get(Product.class, id);
            return prod;
        });
    }

    /**
     * Deletes a product from the database based on the provided product ID.
     *
     * @param id
     *            the ID of the product to be deleted
     */
    @SuppressWarnings("unused")
    public void deleteProduct(Integer id) {
        Objects.requireNonNull(id, "Product ID must not be null");
        HibernateUtil.inTransaction(session -> {
            @Nullable
            Product product = session.get(Product.class, id);
            if (product == null) {
                throw new IllegalArgumentException(
                        "Product with ID " + id + " not found");
            }
            logger.info("Deleting Product: ({})", id);
            session.delete(product);
        });
    }

    /**
     * Retrieves a collection of products that belong to the specified category.
     *
     * @param category
     *            the category for which products are to be fetched
     * @return a collection of products that belong to the specified category
     */
    @SuppressWarnings("java:S1192")
    public Collection<@NonNull Product> getProductsByCategory(
            Category category) {
        Objects.requireNonNull(category, "Category must not be null");
        Objects.requireNonNull(category.getId(),
                "Category ID must not be null");
        logger.info("Fetching Products by Category: ({}) '{}'",
                category.getId(), category.getName());
        var result = HibernateUtil.inSession(session -> {
            return session.createQuery(
                    "select p from Product p join p.category c where c.id = :id",
                    Product.class).setParameter("id", category.getId()).list();
        });
        if (result == null) {
            throw new IllegalStateException(
                    "Result of getProductsByCategory is null, this should not happen");
        }
        return result;
    }

    /**
     * Retrieves all products from the database.
     * 
     * This method uses HibernateUtil to open a session and execute a query that
     * fetches all instances of the Product class from the database.
     * 
     * @return a collection of all products available in the database.
     */
    public Collection<@NonNull Product> getAllProducts() {
        // Method returns all products from the database using HibernateUtil
        logger.info("Fetching all Products");
        var result = HibernateUtil.inSession(session -> {
            // Using LEFT JOIN FETCH to eagerly load associated categories
            // to avoid N+1 select problem
            return session.createQuery(
                    "select distinct p from Product p left join fetch p.category",
                    Product.class).list();
        });
        if (result == null) {
            throw new IllegalStateException(
                    "Result of getAllProducts is null, this should not happen");
        }
        return result;
    }

    /**
     * Updates the given Category in the database. If the Category has an ID, it
     * will be updated. Otherwise, a new Category will be created and its ID
     * will be assigned.
     *
     * @param category
     *            the Category to be updated or created
     * @return the updated or newly created Category
     */
    public Category updateCategory(Category category) {
        Objects.requireNonNull(category,
                "Category to be updated must not be null");
        logger.info("Persisting Category: ({}) '{}'", category.getId(),
                category.getName());
        var identifier = HibernateUtil.inTransaction(session -> {
            Integer id;
            if (category.getId() != null) {
                session.update(category);
                id = category.getId();
            } else {
                id = (Integer) session.save(category);
            }
            return id;
        });
        assert identifier != null;
        var result = HibernateUtil.inSession(session -> {
            @Nullable
            Category cat = session.get(Category.class, identifier);
            return cat;
        });
        if (result == null) {
            throw new IllegalStateException(
                    "Just saved category is now missing: " + identifier);
        }
        return result;
    }

    /**
     * Retrieves a Category object from the database based on the provided ID.
     *
     * @param id
     *            the ID of the Category to be fetched
     * @return the Category object corresponding to the provided ID, or null if
     *         not found
     */
    @Nullable
    public Category getCategory(Integer id) {
        Objects.requireNonNull(id, "Category ID must not be null");
        logger.info("Fetching Category: ({})", id);
        return HibernateUtil.inSession(session -> {
            @Nullable
            Category category = session.get(Category.class, id);
            return category;
        });
    }

    /**
     * Retrieves all categories from the database.
     *
     * @return a collection of all categories.
     */
    public Collection<@NonNull Category> getAllCategories() {
        logger.info("Fetching all Categories");
        var result = HibernateUtil.inSession(session -> {
            return session.createQuery("from Category", Category.class).list();
        });
        if (result == null) {
            throw new IllegalStateException(
                    "Result of getAllCategories is null");
        }
        return result;
    }

    /**
     * Fetches a set of Category objects based on their IDs.
     *
     * @param ids
     *            a set of integer IDs representing the categories to be fetched
     * @return a set of Category objects corresponding to the provided IDs
     */
    public Set<@NonNull Category> getCategoriesByIds(Set<Integer> ids) {
        Objects.requireNonNull(ids, "Category IDs must not be null");
        logger.info("Fetching Categories: {}", ids);
        var result = HibernateUtil.inSession(session -> {
            return session
                    .createQuery("from Category where id in (:ids)",
                            Category.class)
                    .setParameter("ids", ids).list().stream()
                    .collect(Collectors.toSet());
        });
        if (result == null) {
            throw new IllegalStateException(
                    "Result of getCategoriesByIds is null, this should not happen");
        }
        return result;
    }

    /**
     * Deletes a category by its ID. This method performs the following steps:
     * 1. Retrieves the category from the database using the provided ID. 2.
     * Finds all products and drafts associated with the category. 3. For each
     * product and draft, removes the category from its list of categories and
     * updates the product or draft in the database. 4. Deletes the category
     * from the database. All operations are performed within a single
     * transaction.
     *
     * @param id
     *            the ID of the category to be deleted
     */
    @SuppressWarnings("unused")
    public void deleteCategory(Integer id) {
        Objects.requireNonNull(id, "Category ID must not be null");
        HibernateUtil.inTransaction(session -> {
            @Nullable
            Category category = session.get(Category.class, id);
            if (category == null) {
                logger.warn("Category with ID ({}) not found", id);
                return;
            }
            // For many-to-many relationships, we need to remove the association
            // by updating the join table, not the collection directly

            // Remove category from products (assuming product_category join
            // table)
            int updatedProducts = session.createNativeQuery(
                    "DELETE FROM product_category WHERE category_id = :categoryId")
                    .setParameter("categoryId", id).executeUpdate();

            // Remove category from drafts (assuming draft_category join table)
            int updatedDrafts = session.createNativeQuery(
                    "DELETE FROM draft_category WHERE category_id = :categoryId")
                    .setParameter("categoryId", id).executeUpdate();

            // Now safe to delete the category itself
            session.delete(category);

            logger.info(
                    "Deleted Category: ({}) '{}', Updated {} products and {} drafts",
                    category.getId(), category.getName(), updatedProducts,
                    updatedDrafts);

        });
    }

    /**
     * Fetches a Category entity from the database by its name.
     *
     * @param name
     *            the name of the Category to fetch
     * @return the Category entity with the specified name, or null if no such
     *         entity exists
     */
    @Nullable
    public Category getCategoryByName(String name) {
        Objects.requireNonNull(name, "Category name must not be null");
        logger.info("Fetching Category by name: '{}'", name);
        return HibernateUtil.inSession(session -> {
            @Nullable
            Category category = session
                    .createQuery("from Category where name = :name",
                            Category.class)
                    .setParameter("name", name).uniqueResult();
            return category;
        });
    }

    @SuppressWarnings("null")
    private Logger logger = LoggerFactory.getLogger(this.getClass());
}