package org.vaadin.tatu.vaadincreate.crud;

import java.io.Serializable;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

import javax.persistence.OptimisticLockException;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.tatu.vaadincreate.VaadinCreateUI;
import org.vaadin.tatu.vaadincreate.auth.AccessControl;
import org.vaadin.tatu.vaadincreate.backend.ProductDataService;
import org.vaadin.tatu.vaadincreate.backend.data.Category;
import org.vaadin.tatu.vaadincreate.backend.data.Product;
import org.vaadin.tatu.vaadincreate.backend.data.User.Role;
import org.vaadin.tatu.vaadincreate.backend.events.AbstractEvent;
import org.vaadin.tatu.vaadincreate.backend.events.BooksChangedEvent;
import org.vaadin.tatu.vaadincreate.backend.events.BooksChangedEvent.BookChange;
import org.vaadin.tatu.vaadincreate.backend.events.LockingEvent;
import org.vaadin.tatu.vaadincreate.eventbus.EventBus;
import org.vaadin.tatu.vaadincreate.eventbus.EventBus.EventBusListener;
import org.vaadin.tatu.vaadincreate.locking.LockedObjects;
import org.vaadin.tatu.vaadincreate.util.Utils;

/**
 * This class provides an interface for the logical operations between the CRUD
 * view, its parts like the product editor form and the data source, including
 * fetching and saving products.
 *
 * Having this separate from the view makes it easier to test various parts of
 * the system separately, and to e.g. provide alternative views for the same
 * data.
 */
@NullMarked
@SuppressWarnings("serial")
public class BooksPresenter implements Serializable, EventBusListener {

    private static final Logger logger = LoggerFactory
            .getLogger(BooksPresenter.class);

    private final BooksView view;
    @Nullable
    private transient CompletableFuture<Void> future;
    private final AccessControl accessControl = VaadinCreateUI.get()
            .getAccessControl();

    @Nullable
    private Product editing;
    @Nullable
    private Product draft;

    /**
     * Creates a new instance of the presenter.
     *
     * @param booksView
     *            the view
     */
    public BooksPresenter(BooksView booksView) {
        view = booksView;
        getEventBus().registerEventBusListener(this);
    }

    // This method is used to load products asynchronously.
    private CompletableFuture<Collection<Product>> loadProductsAsync() {
        logger.info("Fetching products");
        var service = getService();
        return CompletableFuture.supplyAsync(service::getAllProducts,
                getExecutor());
    }

    /**
     * Requests an update of the categories from the service and sets them in
     * the view. Logs the action of fetching categories.
     */
    public void requestUpdateCategories() {
        logger.info("Fetching categories");
        var categories = getService().getAllCategories();
        view.setCategories(categories);
    }

    /**
     * Validates the given set of categories against the available categories
     * from the service. If any of the given categories are not present in the
     * available categories, it updates the view to show that some categories
     * have been deleted and resets the categories in the view.
     *
     * @param categories
     *            the set of categories to validate
     * @return true if all given categories are valid and present in the
     *         available categories, false otherwise
     */
    public boolean validateCategories(Set<Category> categories) {
        var allCategories = getService().getAllCategories();
        var valid = allCategories.containsAll(categories);
        if (!valid) {
            view.showCategoriesDeleted();
            view.setCategories(allCategories);
        }
        return valid;
    }

    /**
     * Requests an update of the products and updates the view asynchronously.
     */
    public void requestUpdateProducts() {
        future = loadProductsAsync().thenAccept(products -> {
            logger.info("Fetching products complete");
            view.setProductsAsync(products);
            future = null;
        });
    }

    /**
     * Cancels the update of products and unlocks the book. If there is a future
     * task running, it will be cancelled and the future reference will be set
     * to null.
     */
    public void cancelUpdateProducts() {
        unlockBook();
        getEventBus().unregisterEventBusListener(this);
        if (future != null) {
            boolean cancelled = future.cancel(true);
            future = null;
            logger.info("Fetching products cancelled: {}", cancelled);
        }
    }

    /**
     * Initializes the BooksPresenter.
     *
     * This method is responsible for initializing the presenter and setting up
     * the initial state of the view. It calls the editProduct method with a
     * null parameter to ensure that the view is in an editable state. If the
     * current user is not an admin, it disables the ability to create new
     * products.
     */
    public void init() {
        editProduct(null);
        // Hide and disable if not admin
        if (!accessControl.isUserInRole(Role.ADMIN)) {
            view.setNewProductEnabled(false);
        }
    }

    /**
     * Cancels the current product operation. This method calls the
     * `cancelProduct` method of the view and unlocks the book.
     */
    public void cancelProduct() {
        view.cancelProduct();
    }

    /**
     * Unlocks the currently editing book. If there is a book currently being
     * edited, it will be unlocked by calling the `unlock` method of the
     * `lockedBooks` object. After unlocking the book, the `editing` variable is
     * set to null.
     */
    public void unlockBook() {
        if (editing != null) {
            getLockedBooks().unlock(editing);
            editing = null;
        }
    }

    /**
     * Locks a book with the specified ID for editing. If there is already a
     * book being edited, it will be unlocked first.
     *
     * @param id
     *            the ID of the book to lock for editing
     */
    private void lockBook(Product book) {
        assert book != null : "Book must not be null";
        var user = Utils.getCurrentUserOrThrow();
        if (editing != null) {
            unlockBook();
        }
        getLockedBooks().lock(book, user);
        editing = book;
    }

    /**
     * Handles the navigation to the view with the specified product ID. If the
     * product ID is "new", it creates a new product. Otherwise, it attempts to
     * find the product with the given ID and selects it in the view. If the
     * product ID is not valid or cannot be parsed as an integer, an error
     * message is shown in the view.
     *
     * @param productId
     *            the ID of the product to navigate to
     */
    public void enter(@Nullable String productId) {
        if (!accessControl.isUserInRole(Role.ADMIN)) {
            view.setFragmentParameter("");
            return;
        }
        if (productId != null && !productId.isEmpty()) {
            if (productId.equals("new")) {
                newProduct();
            } else {
                try {
                    lockAndEditIfExistsAndIsUnlocked(productId);
                } catch (NumberFormatException e) {
                    view.showNotValidId(productId);
                    logger.warn("Attempt to edit invalid id '{}'", productId);
                }
            }
        }
    }

    // This method is used to lock and edit a product if it exists and is
    // unlocked. If the product exists and is not locked, it will be locked and
    // selected in the view. If the product does not exist, a notification will
    // be shown in the view. If the product exists but is locked, a notification
    // will be shown in the view.
    private void lockAndEditIfExistsAndIsUnlocked(String productId) {
        assert productId != null;

        int pid = Integer.parseInt(productId);
        Product product = findProduct(pid);
        if (product != null) {
            if (getLockedBooks().isLocked(product) == null) {
                lockBook(product);
                // Ensure this is selected even if coming directly here from
                // login
                view.selectRow(product);
            } else {
                view.showProductLocked(productId);
                logger.warn("Attempt to edit locked product '{}'", productId);
            }
        } else {
            if (getDraft() != null) {
                view.showDeleteNotification(null);
                newProduct();
            } else {
                view.showNotValidId(productId);
                logger.warn("Attempt to edit invalid id '{}'", productId);
            }
        }
    }

    /**
     * Finds a product by its ID.
     *
     * @param productId
     *            the ID of the product to find
     * @return the product with the specified ID, or null if not found
     */
    @Nullable
    public Product findProduct(int productId) {
        logger.info("Fetching product {}", productId);
        return getService().getProductById(productId);
    }

    /**
     * Saves the given product
     *
     * @param product
     *            The product to be saved, not null.
     */
    @Nullable
    public Product saveProduct(Product product) {
        Objects.requireNonNull(product, "Product must not be null");

        accessControl.assertAdmin();
        view.clearSelection();
        boolean newBook = product.getId() == null;
        logger.info("Saving product: {}", newBook ? "new" : product.getId());

        try {
            product = getService().updateProduct(product);
        } catch (OptimisticLockException e) {
            logger.warn(
                    "Optimistic lock happened, this should not happen in BooksView");
            view.showInternalError();
            return null;
        } catch (IllegalArgumentException e) {
            logger.error("Backend service failure while updating product: {}",
                    e.getMessage());
            view.showInternalError();
            return null;
        }
        view.showSaveNotification(product.getProductName());
        view.setNewProductEnabled(true);

        if (newBook) {
            // Add new product to the view
            view.updateGrid(product);
        } else {
            // Update product in the view
            view.updateProduct(product);
            unlockBook();
        }
        view.setFragmentParameter("");
        // Post SaveEvent to EventBus
        var id = product.getId();
        assert id != null : "Product ID should not be null";
        getEventBus().post(
                new BooksChangedEvent(id, BooksChangedEvent.BookChange.SAVE));
        return product;
    }

    /**
     * Deletes a product from the system.
     *
     * @param product
     *            the product to be deleted
     */
    public void deleteProduct(Product product) {
        accessControl.assertAdmin();
        var id = product.getId();
        Objects.requireNonNull(id, "Product must have an ID to be deleted");
        view.showDeleteNotification(product.getProductName());
        view.clearSelection();
        view.setNewProductEnabled(true);
        logger.info("Deleting product: {}", product.getId());
        getService().deleteProduct(id);
        view.removeProduct(product);
        unlockBook();
        view.setFragmentParameter("");
        getEventBus().post(
                new BooksChangedEvent(id, BooksChangedEvent.BookChange.DELETE));
    }

    /**
     * Edits the specified product. If the product is null, it sets the fragment
     * parameter to an empty string and unlocks the book. If the product's ID is
     * -1, it sets the fragment parameter to "new". Otherwise, it sets the
     * fragment parameter to the product's ID and locks the book.
     *
     * @param product
     *            the product to be edited
     */
    public void editProduct(@Nullable Product product) {
        if (product == null) {
            view.setFragmentParameter("");
            unlockBook();
        } else if (product.getId() == null) {
            view.setFragmentParameter("new");
        } else {
            view.setFragmentParameter(product.getId() + "");
            lockBook(product);
        }
        logger.info("Editing product: {}",
                product != null ? product.getId() : "none");
        view.editProduct(product);
    }

    /**
     * Creates a new product. This method clears the selection, sets the
     * fragment parameter to "new", logs an info message, and edits a new
     * product in the view.
     */
    public void newProduct() {
        if (accessControl.isUserInRole(Role.ADMIN)) {
            view.clearSelection();
            view.setFragmentParameter("new");
            logger.info("New product");
            view.newProduct();
        } else {
            view.setFragmentParameter("");
        }
    }

    /**
     * Handles the event when a row is selected in the UI. If the user has the
     * role of ADMIN and the selected product is not locked, the product is
     * edited. Otherwise, the selection is cleared.
     *
     * @param product
     *            the selected product
     */
    public void rowSelected(@Nullable Product product) {
        if (accessControl.isUserInRole(Role.ADMIN)) {
            if (product != null && getLockedBooks().isLocked(product) != null) {
                view.clearSelection();
            } else {
                editProduct(product);
            }
        }
    }

    /**
     * Selects a product and notifies the view of the selection change.
     *
     * @param product
     *            the product to be selected
     */
    public void selectProduct(Product product) {
        view.handleSelectionChange(product);
    }

    /**
     * Saves the given product draft.
     *
     * @param draft
     *            the product draft to be saved
     * @throws IllegalStateException
     *             if the current user is not an admin
     * @throws IllegalStateException
     *             if the current user is not present in the context
     */
    public void saveDraft(Product draft) {
        accessControl.assertAdmin();
        var user = Utils.getCurrentUserOrThrow();
        getService().saveDraft(user, draft);
        this.draft = draft;
    }

    /**
     * Removes the current draft.
     *
     * @throws IllegalStateException
     *             if the current user is not an admin
     * @throws IllegalStateException
     *             if the current user is not present in the context
     */
    public void removeDraft() {
        accessControl.assertAdmin();
        var user = Utils.getCurrentUserOrThrow();
        draft = null;
        getService().saveDraft(user, null);
    }

    /**
     * Retrieves the draft version of a product for the currently authenticated
     * user.
     *
     * @return the draft product associated with the current user's principal
     *         name.
     */
    @Nullable
    public Product getDraft() {
        var user = Utils.getCurrentUserOrThrow();
        if (draft == null) {
            draft = getService().findDraft(user);
        }
        return draft;
    }

    @Override
    public void eventFired(AbstractEvent event) {
        switch (event) {
        case LockingEvent lockingEvent -> {
            var id = lockingEvent.id();
            view.refreshProductAsync(id);
        }
        case BooksChangedEvent booksChanged -> {
            if (booksChanged.change() != BookChange.SAVE) {
            return;
            }
            var id = booksChanged.productId();
            view.refreshProductAsync(id);
        }
        default -> {
            // No action
        }
        }
    }

    private LockedObjects getLockedBooks() {
        return LockedObjects.get();
    }

    private ProductDataService getService() {
        return VaadinCreateUI.get().getProductService();
    }

    private ExecutorService getExecutor() {
        return VaadinCreateUI.get().getExecutor();
    }

    private EventBus getEventBus() {
        return EventBus.get();
    }

}
