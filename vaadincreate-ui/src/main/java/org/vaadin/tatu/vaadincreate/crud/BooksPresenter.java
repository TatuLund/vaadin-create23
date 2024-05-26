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
    private LockedBooks lockedBooks = LockedBooks.get();
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

    public void requestUpdateProducts() {
        future = loadProductsAsync().thenAccept(products -> {
            logger.info("Fetching products complete");
            view.setProductsAsync(products);
            future = null;
        });
    }

    public void cancelUpdateProducts() {
        unlockBook();
        if (future != null) {
            boolean cancelled = future.cancel(true);
            future = null;
            logger.info("Fetching products cancelled: {}", cancelled);
        }
    }

    public void init() {
        editProduct(null);
        // Hide and disable if not admin
        if (!accessControl.isUserInRole(Role.ADMIN)) {
            view.setNewProductEnabled(false);
        }
    }

    public void cancelProduct() {
        view.cancelProduct();
        unlockBook();
    }

    private void unlockBook() {
        if (editing != null) {
            lockedBooks.unlock(editing);
            editing = null;
        }
    }

    private void lockBook(Integer id) {
        if (editing != null) {
            unlockBook();
        }
        lockedBooks.lock(id);
        editing = id;
    }

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

    private Product findProduct(int productId) {
        return service.getProductById(productId);
    }

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

    public void deleteProduct(Product product) {
        view.showDeleteNotification(product.getProductName());
        view.clearSelection();
        logger.info("Deleting product: {}", product.getId());
        service.deleteProduct(product.getId());
        view.removeProduct(product);
        unlockBook();
        view.setFragmentParameter("");
    }

    public void editProduct(Product product) {
        if (product == null) {
            view.setFragmentParameter("");
            unlockBook();
        } else {
            view.setFragmentParameter(product.getId() + "");
            lockBook(product.getId());
        }
        logger.info("Editing product: {}",
                product != null ? product.getId() : "none");
        view.editProduct(product);
    }

    public void newProduct() {
        view.clearSelection();
        view.setFragmentParameter("new");
        logger.info("New product");
        view.editProduct(new Product());
    }

    public void rowSelected(Product product) {
        if (accessControl.isUserInRole(Role.ADMIN)) {
            if (product != null
                    && lockedBooks.lockedBooks().contains(product.getId())) {
                view.clearSelection();
            } else {
                editProduct(product);
            }
        }
    }

    private Logger logger = LoggerFactory.getLogger(this.getClass());
}
