package org.vaadin.tatu.vaadincreate.crud;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.tatu.vaadincreate.VaadinCreateUI;
import org.vaadin.tatu.vaadincreate.backend.ProductDataService;
import org.vaadin.tatu.vaadincreate.backend.data.Category;
import org.vaadin.tatu.vaadincreate.backend.data.Product;
import org.vaadin.tatu.vaadincreate.backend.data.User.Role;

import java.io.Serializable;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
    private CompletableFuture<Void> future;

    public BooksPresenter(BooksView simpleCrudView) {
        view = simpleCrudView;
    }

    public CompletableFuture<Collection<Product>> loadProductsAsync() {
        logger.info("Fetching products");
        return CompletableFuture.supplyAsync(
                () -> ProductDataService.get().getAllProducts(), executor);
    }

    public CompletableFuture<Collection<Category>> loadCategoriesAsync() {
        return CompletableFuture.supplyAsync(
                () -> ProductDataService.get().getAllCategories(), executor);
    }

    public void updateProduct(Product product) {
        ProductDataService.get().updateProduct(product);
    }

    public void requestUpdateProducts() {
        future = loadProductsAsync()
                .thenAccept(products -> view.setProductsAsync(products));
    }

    public void cancelUpdateProducts() {
        if (future != null) {
            boolean cancelled = future.cancel(true);
            future = null;
            logger.info("Fetching products cancelled: " + cancelled);
        }
    }

    public void init() {
        editProduct(null);
        // Hide and disable if not admin
        if (!VaadinCreateUI.get().getAccessControl().isUserInRole(Role.ADMIN)) {
            view.setNewProductEnabled(false);
        }
    }

    public void cancelProduct() {
        setFragmentParameter("");
        view.clearSelection();
    }

    /**
     * Update the fragment without causing navigator to change view
     */
    private void setFragmentParameter(String productId) {
        String fragmentParameter;
        if (productId == null || productId.isEmpty()) {
            fragmentParameter = "";
        } else {
            fragmentParameter = productId;
        }

        var page = VaadinCreateUI.get().getPage();
        page.setUriFragment("!" + BooksView.VIEW_NAME + "/" + fragmentParameter,
                false);
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
                    view.selectRow(product);
                } catch (NumberFormatException e) {
                }
            }
        }
    }

    private Product findProduct(int productId) {
        return ProductDataService.get().getProductById(productId);
    }

    public void saveProduct(Product product) {
        view.showSaveNotification(product.getProductName() + " ("
                + product.getId() + ") updated");
        view.clearSelection();
        logger.info("Saving product: {}",
                product.getId() == -1 ? "new" : product.getId());
        ProductDataService.get().updateProduct(product);
        view.updateProduct(product);
        setFragmentParameter("");
    }

    public void deleteProduct(Product product) {
        view.showSaveNotification(product.getProductName() + " ("
                + product.getId() + ") removed");
        view.clearSelection();
        logger.info("Deleting product: {}", product.getId());
        ProductDataService.get().deleteProduct(product.getId());
        view.removeProduct(product);
        setFragmentParameter("");
    }

    public void editProduct(Product product) {
        if (product == null) {
            setFragmentParameter("");
        } else {
            setFragmentParameter(product.getId() + "");
        }
        logger.info("Editing product: {}",
                product != null ? product.getId() : "none");
        view.editProduct(product);
    }

    public void newProduct() {
        view.clearSelection();
        setFragmentParameter("new");
        logger.info("New product");
        view.editProduct(new Product());
    }

    public void rowSelected(Product product) {
        // if (product == null) {
        // view.editProduct(null);
        // return;
        // }
        if (VaadinCreateUI.get().getAccessControl().isUserInRole(Role.ADMIN)) {
            editProduct(product);
        }
    }

    private Logger logger = LoggerFactory.getLogger(this.getClass());
}
