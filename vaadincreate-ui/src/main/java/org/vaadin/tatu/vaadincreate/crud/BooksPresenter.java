package org.vaadin.tatu.vaadincreate.crud;

import java.io.Serializable;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.tatu.vaadincreate.VaadinCreateUI;
import org.vaadin.tatu.vaadincreate.auth.AccessControl;
import org.vaadin.tatu.vaadincreate.auth.CurrentUser;
import org.vaadin.tatu.vaadincreate.backend.ProductDataService;
import org.vaadin.tatu.vaadincreate.backend.data.Category;
import org.vaadin.tatu.vaadincreate.backend.data.Product;
import org.vaadin.tatu.vaadincreate.backend.data.User.Role;
import org.vaadin.tatu.vaadincreate.locking.LockedObjects;

/**
 * This class provides an interface for the logical operations between the CRUD
 * view, its parts like the product editor form and the data source, including
 * fetching and saving products.
 *
 * Having this separate from the view makes it easier to test various parts of
 * the system separately, and to e.g. provide alternative views for the same
 * data.
 */
@SuppressWarnings("serial")
public class BooksPresenter implements Serializable {

    private BooksView view;
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private transient CompletableFuture<Void> future;
    private ProductDataService service = VaadinCreateUI.get()
            .getProductService();
    private AccessControl accessControl = VaadinCreateUI.get()
            .getAccessControl();
    private LockedObjects lockedBooks = LockedObjects.get();
    private Integer editing;

    public BooksPresenter(BooksView simpleCrudView) {
        view = simpleCrudView;
    }

    private CompletableFuture<Collection<Product>> loadProductsAsync() {
        logger.info("Fetching products");
        return CompletableFuture.supplyAsync(() -> service.getAllProducts(),
                executor);
    }

    private CompletableFuture<Collection<Category>> loadCategoriesAsync() {
        return CompletableFuture.supplyAsync(() -> service.getAllCategories(),
                executor);
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
            lockedBooks.unlock(Product.class, editing);
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
    private void lockBook(Integer id) {
        if (editing != null) {
            unlockBook();
        }
        lockedBooks.lock(Product.class, id, CurrentUser.get().get());
        editing = id;
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
    public void enter(String productId) {
        if (productId != null && !productId.isEmpty()) {
            if (productId.equals("new")) {
                newProduct();
            } else {
                // Ensure this is selected even if coming directly here from
                // login
                try {
                    int pid = Integer.parseInt(productId);
                    Product product = findProduct(pid);
                    if (product != null) {
                        view.selectRow(product);
                    } else {
                        view.showNotValidId(productId);
                    }
                } catch (NumberFormatException e) {
                    view.showNotValidId(productId);
                }
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
    public Product findProduct(int productId) {
        logger.info("Fetching product {}", productId);
        return service.getProductById(productId);
    }

    /**
     * Saves the given product.
     *
     * @param product
     *            The product to be saved.
     */
    public void saveProduct(Product product) {
        view.showSaveNotification(product.getProductName());
        view.clearSelection();
        boolean newBook = product.getId() == -1;
        logger.info("Saving product: {}", newBook ? "new" : product.getId());
        var savedProduct = service.updateProduct(product);
        if (newBook) {
            view.updateGrid(savedProduct);
        } else {
            view.updateProduct(savedProduct);
            unlockBook();
        }
        view.setFragmentParameter("");
    }

    /**
     * Deletes a product from the system.
     *
     * @param product
     *            the product to be deleted
     */
    public void deleteProduct(Product product) {
        view.showDeleteNotification(product.getProductName());
        view.clearSelection();
        logger.info("Deleting product: {}", product.getId());
        service.deleteProduct(product.getId());
        view.removeProduct(product);
        unlockBook();
        view.setFragmentParameter("");
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
    public void editProduct(Product product) {
        if (product == null) {
            view.setFragmentParameter("");
            unlockBook();
        } else if (product.getId() == -1) {
            view.setFragmentParameter("new");
        } else {
            view.setFragmentParameter(product.getId() + "");
            lockBook(product.getId());
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
        view.clearSelection();
        view.setFragmentParameter("new");
        logger.info("New product");
        view.editProduct(new Product());
    }

    /**
     * Handles the event when a row is selected in the UI. If the user has the
     * role of ADMIN and the selected product is not locked, the product is
     * edited. Otherwise, the selection is cleared.
     *
     * @param product
     *            the selected product
     */
    public void rowSelected(Product product) {
        if (accessControl.isUserInRole(Role.ADMIN)) {
            if (product != null && lockedBooks.isLocked(Product.class,
                    product.getId()) != null) {
                view.clearSelection();
            } else {
                editProduct(product);
            }
        }
    }

    private Logger logger = LoggerFactory.getLogger(this.getClass());
}
