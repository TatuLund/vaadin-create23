package org.vaadin.tatu.vaadincreate.crud;

import java.util.Collection;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.tatu.vaadincreate.AttributeExtension;
import org.vaadin.tatu.vaadincreate.ConfirmDialog;
import org.vaadin.tatu.vaadincreate.VaadinCreateTheme;
import org.vaadin.tatu.vaadincreate.VaadinCreateUI;
import org.vaadin.tatu.vaadincreate.VaadinCreateView;
import org.vaadin.tatu.vaadincreate.AttributeExtension.AriaAttributes;
import org.vaadin.tatu.vaadincreate.auth.AccessControl;
import org.vaadin.tatu.vaadincreate.auth.RolesPermitted;
import org.vaadin.tatu.vaadincreate.backend.data.Category;
import org.vaadin.tatu.vaadincreate.backend.data.Product;
import org.vaadin.tatu.vaadincreate.backend.data.User.Role;
import org.vaadin.tatu.vaadincreate.crud.form.BookForm;
import org.vaadin.tatu.vaadincreate.i18n.I18n;
import org.vaadin.tatu.vaadincreate.util.Utils;

import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.event.ShortcutAction.ModifierKey;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.navigator.ViewBeforeLeaveEvent;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.Grid.SelectionMode;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

/**
 * A view for performing create-read-update-delete operations on products.
 *
 * See also {@link BookPresenter} for fetching the data, the actual CRUD
 * operations and controlling the view based on events from outside.
 */
@NullMarked
@SuppressWarnings({ "serial", "java:S2160" })
@RolesPermitted({ Role.USER, Role.ADMIN })
public class BooksView extends CssLayout implements VaadinCreateView {

    private static final Logger logger = LoggerFactory
            .getLogger(BooksView.class);

    public static final String VIEW_NAME = "inventory";

    private BookGrid grid;
    private BookForm form;

    private BooksPresenter presenter = new BooksPresenter(this);
    private AccessControl accessControl = VaadinCreateUI.get()
            .getAccessControl();

    private Button newProduct = new Button(
            getTranslation(I18n.Books.NEW_PRODUCT));

    private FakeGrid fakeGrid;
    private NoMatches noMatches = new NoMatches();

    @Nullable
    private String params;

    @Nullable
    private Product draft;

    @Nullable
    private UI ui;

    @Nullable
    private FilterField filterField;

    public BooksView() {
        setSizeFull();
        addStyleName(VaadinCreateTheme.BOOKVIEW);
        var topLayout = createTopBar();

        noMatches.setVisible(false);

        grid = new BookGrid();
        grid.asSingleSelect().addValueChangeListener(selected -> {
            if (selected.isUserOriginated()) {
                handleSelectionChange(selected.getValue());
            }
        });
        grid.setVisible(false);

        // Display fake Grid while loading data
        fakeGrid = new FakeGrid();

        form = new BookForm(presenter, grid);

        var barAndGridLayout = new VerticalLayout();
        var gridWrapper = new CssLayout();
        gridWrapper.setSizeFull();
        gridWrapper.addStyleName(VaadinCreateTheme.BOOKVIEW_GRIDWRAPPER);
        gridWrapper.addComponents(noMatches, grid, fakeGrid);
        barAndGridLayout.addComponents(topLayout, gridWrapper);
        barAndGridLayout.setSizeFull();
        barAndGridLayout.setExpandRatio(gridWrapper, 1);
        barAndGridLayout.setStyleName(VaadinCreateTheme.BOOKVIEW_GRID);

        addComponent(barAndGridLayout);
        addComponent(form);

        presenter.init();
    }

    public void handleSelectionChange(@Nullable Product product) {
        if (form.hasChanges()) {
            var dialog = createDiscardChangesConfirmDialog();
            dialog.open();
            dialog.addConfirmedListener(confirmed -> {
                presenter.unlockBook();
                form.showForm(false);
                setFragmentParameter("");
            });
            dialog.addCancelledListener(
                    cancelled -> grid.select(form.getProduct()));
        } else {
            presenter.rowSelected(product);
            if (product != null) {
                grid.select(product);
            }
        }
    }

    // Filter the grid data based on the filter text
    private static <T> boolean filterCondition(T value, String filterText) {
        assert filterText != null : "Filter text cannot be null";
        assert value != null : "Value cannot be null";

        return value.toString().toLowerCase()
                .contains(filterText.toLowerCase());
    }

    /**
     * Checks if a given book passes the filter based on the filter text.
     *
     * @param book
     *            The book to be checked.
     * @param filterText
     *            The filter text to be applied.
     * @return {@code true} if the book passes the filter, {@code false}
     *         otherwise.
     */
    private boolean passesFilter(Product book, String filterText) {
        assert filterText != null : "Filter text cannot be null";
        assert book != null : "Book cannot be null";

        filterText = filterText.trim();
        return filterCondition(book.getProductName(), filterText)
                || filterCondition(book.getAvailability(), filterText)
                || filterCondition(book.getCategory(), filterText);
    }

    /**
     * Creates a horizontal layout for the top bar of the BooksView. The top bar
     * contains a filter TextField and a newProduct Button. The filter TextField
     * is used to filter the data in the grid. The newProduct Button is used to
     * create a new product.
     *
     * @return the created HorizontalLayout for the top bar
     */
    private HorizontalLayout createTopBar() {
        filterField = new FilterField();
        // Apply the filter to grid's data provider. TextField value is never
        // null
        filterField.addValueChangeListener(valueChange -> {
            grid.setFilter(book -> passesFilter(book, valueChange.getValue()));
            updateNoMatchesVisibility();
        });

        newProduct.setId("new-product");
        newProduct.setEnabled(false);
        newProduct.addStyleName(ValoTheme.BUTTON_PRIMARY);
        newProduct.setIcon(VaadinIcons.PLUS_CIRCLE);
        newProduct.addClickListener(click -> presenter.newProduct());
        newProduct.setClickShortcut(KeyCode.N, ModifierKey.ALT);
        AttributeExtension.of(newProduct)
                .setAttribute(AriaAttributes.KEYSHORTCUTS, "Alt+N");

        var topLayout = new HorizontalLayout();
        topLayout.setWidth("100%");
        topLayout.addComponents(filterField, newProduct);
        topLayout.setComponentAlignment(filterField, Alignment.MIDDLE_LEFT);
        topLayout.setExpandRatio(filterField, 1);
        topLayout.setStyleName(VaadinCreateTheme.BOOKVIEW_TOOLBAR);
        return topLayout;
    }

    private void updateNoMatchesVisibility() {
        noMatches.setVisible(grid.isEmpty());
    }

    @Override
    public void attach() {
        super.attach();
        ui = getUI();
    }

    @Override
    public void enter(ViewChangeEvent viewChange) {
        openingView(VIEW_NAME);
        draft = presenter.getDraft();
        params = viewChange.getParameters();
        if (!accessControl.isUserInRole(Role.ADMIN)) {
            grid.setSelectionMode(SelectionMode.NONE);
            grid.setReadOnly(true);
            form.setVisible(false);
        } else {
            form.setVisible(false);
        }
        presenter.requestUpdateProducts();
    }

    public void setCategories(Collection<Category> categories) {
        form.setCategories(categories);
    }

    /**
     * Cancels the product editing and discards any changes made. If there are
     * unsaved changes, a confirmation dialog is displayed. If the changes are
     * confirmed, the form is hidden, the selection is cleared, and the fragment
     * parameter is set to an empty string. If there are no unsaved changes, the
     * form is hidden and the selection is cleared.
     */
    public void cancelProduct() {
        if (form.hasChanges()) {
            var dialog = createDiscardChangesConfirmDialog();
            dialog.open();
            dialog.addConfirmedListener(confirmed -> {
                form.showForm(false);
                clearSelection();
                setFragmentParameter("");
                presenter.unlockBook();
                grid.focus();
            });
        } else {
            form.showForm(false);
            clearSelection();
            setFragmentParameter("");
            presenter.unlockBook();
            grid.focus();
        }
    }

    /**
     * Set Grid's DataProvider to use collection of products as data. The update
     * is done in {@link UI#access(Runnable)} wrapping in order to be thread
     * safe and ensure locking of the UI during update.
     *
     * @param products
     *            Collection of Product
     */
    public void setProductsAsync(Collection<Product> products) {
        Utils.access(ui, () -> {
            if (accessControl.isUserInRole(Role.ADMIN)) {
                form.setVisible(true);
                if (params == null || params.isEmpty()) {
                    form.showForm(false);
                }
                newProduct.setEnabled(true);
            }
            logger.info("Updating products");
            grid.setItems(products);
            grid.setVisible(true);
            fakeGrid.setVisible(false);
            // Open form with url parameter based book
            presenter.enter(params);
        });
    }

    /**
     * Displays an error message using a Vaadin notification.
     *
     * @param msg
     *            the error message to be displayed
     */
    public void showError(String msg) {
        Notification.show(msg, Type.ERROR_MESSAGE);
    }

    /**
     * Displays an error message indicating that the provided product ID is not
     * valid.
     *
     * @param productId
     *            the invalid product ID
     */
    public void showNotValidId(String productId) {
        form.showForm(false);
        showError(getTranslation(I18n.Books.NOT_VALID_PID, productId));
    }

    /**
     * Displays an error message indicating that the provided product is locked.
     *
     * @param productId
     *            the lccked product ID
     */
    public void showProductLocked(String productId) {
        showError(getTranslation(I18n.Books.PRODUCT_LOCKED, productId));
    }

    /**
     * Displays an error message indicating that some of the selected categories
     * were deleted.
     */
    public void showCategoriesDeleted() {
        showError(getTranslation(I18n.Books.CATEGORIES_DELETED));
    }

    /**
     * Displays an error message indicating that internal error occurred.
     */
    public void showInternalError() {
        form.showForm(false);
        showError(getTranslation(I18n.Books.INTERNAL_ERROR));
    }

    /**
     * Shows a notification indicating that a book has been saved.
     *
     * @param book
     *            the name of the saved book
     */
    public void showSaveNotification(String book) {
        Notification.show(getTranslation(I18n.Books.UPDATED, book),
                Type.TRAY_NOTIFICATION);
    }

    /**
     * Shows a notification indicating that a book has been deleted.
     *
     * @param book
     *            the name of the book that has been deleted
     */
    public void showDeleteNotification(@Nullable String book) {
        String message = "";
        if (book != null) {
            message = getTranslation(I18n.Books.REMOVED, book);
        } else {
            message = getTranslation(I18n.Books.PRODUCT_DELETED);
        }
        Notification.show(message, Type.TRAY_NOTIFICATION);
    }

    /**
     * Sets the enabled state of the "New Product" button.
     *
     * @param enabled
     *            true to enable the button, false to disable it
     */
    public void setNewProductEnabled(boolean enabled) {
        newProduct.setEnabled(enabled);
    }

    /**
     * Clears the selection in the grid.
     */
    public void clearSelection() {
        // IMHO: UI#access should not be necessary here
        ui.access(() -> grid.deselectAll());
    }

    /**
     * Selects the specified row in the grid. And open editor for it.
     *
     * @param row
     *            the row to be selected
     */
    public void selectRow(Product row) {
        grid.getSelectionModel().select(row);
        editProduct(row);
        grid.focus();
    }

    /**
     * Returns the selected row from the grid as a {@link Product} object.
     *
     * @return the selected row as a {@link Product} object, or null if no row
     *         is selected
     */
    @Nullable
    public Product getSelectedRow() {
        return grid.getSelectedRow();
    }

    /**
     * Updates the specified product in the grid and refreshes the data
     * provider. After updating the product, the form is hidden.
     *
     * @param product
     *            the product to be updated
     */
    public void updateProduct(Product product) {
        logger.info("Refresh item");
        grid.setEdited(product);
        grid.refresh(product);
        form.showForm(false);
        grid.focus();
        updateNoMatchesVisibility();
        // IMHO this should not be necessary
        ui.push();
    }

    /**
     * Updates the grid with a new product and performs necessary UI actions.
     *
     * @param product
     *            the product to be added to the grid
     */
    public void updateGrid(Product product) {
        logger.info("Refresh grid");
        grid.addProduct(product);
        form.showForm(false);
        grid.focus();
        updateNoMatchesVisibility();
        grid.scrollToEnd();
    }

    /**
     * Removes a product from the data provider and refreshes the view.
     *
     * @param product
     *            the product to be removed
     */
    public void removeProduct(Product product) {
        grid.removeProduct(product);
        updateNoMatchesVisibility();
    }

    public void newProduct() {
        editProduct(new Product());
        form.focus();
    }

    /**
     * Edits the specified product.
     *
     * @param product
     *            the product to be edited
     */
    public void editProduct(@Nullable Product product) {
        if (!accessControl.isUserInRole(Role.ADMIN)) {
            return;
        }
        grid.setEdited(null);
        if (product != null) {
            // Ensure the product is up-to-date
            if (product.getId() != null) {
                product = updateProductInGrid(product);
                if (product == null) {
                    showError(getTranslation(I18n.Books.PRODUCT_DELETED));
                    return;
                }
            }
            form.showForm(true);
        } else {
            form.showForm(false);
        }
        form.editProduct(product);
        if (draft != null) {
            form.mergeDraft(draft);
            draft = null;
            presenter.removeDraft();
        }
    }

    @Override
    public void detach() {
        super.detach();
        // If detach happens before completion of data fetch, cancel the fetch
        presenter.cancelUpdateProducts();
        if (getUI().getSession().getSession() == null) {
            presenter.unlockBook();
        }
    }

    /**
     * Update the fragment without causing navigator to change view
     *
     * @param productId
     *            The parameter
     */
    public void setFragmentParameter(@Nullable String productId) {
        String fragmentParameter;
        if (productId == null || productId.isEmpty()) {
            fragmentParameter = "";
        } else {
            fragmentParameter = productId;
        }

        var page = VaadinCreateUI.get().getPage();
        var path = String.format("!%s/%s", BooksView.VIEW_NAME,
                fragmentParameter);
        page.setUriFragment(path, false);
    }

    @Override
    public void beforeLeave(ViewBeforeLeaveEvent event) {
        if (form.hasChanges()) {
            var dialog = createDiscardChangesConfirmDialog();
            dialog.open();
            dialog.addConfirmedListener(confirmed -> {
                form.showForm(false);
                event.navigate();
            });
            // IMHO: Navigator clears url too early and this workaround
            // shouldn't be necessary. This is a possible bug.
            var book = getSelectedRow();
            if (book != null) {
                ui.access(() -> {
                    logger.debug("Set fragment: {}", book.getId());
                    setFragmentParameter("" + book.getId());
                });
            }
        } else {
            event.navigate();
        }
    }

    /**
     * Creates a confirmation dialog for displaying unsaved changes.
     *
     * @return The created ConfirmDialog instance.
     */
    private ConfirmDialog createDiscardChangesConfirmDialog() {
        var dialog = new ConfirmDialog(getTranslation(I18n.CONFIRM),
                getTranslation(I18n.Books.UNSAVED_CHANGES),
                ConfirmDialog.Type.ALERT);
        dialog.setConfirmText(getTranslation(I18n.CONFIRM));
        dialog.setCancelText(getTranslation(I18n.CANCEL));
        return dialog;
    }

    /**
     * Refreshes the product asynchronously by its ID. This method uses the
     * provided UI access utility to ensure that the refresh operation is
     * performed in a thread-safe manner. If the UI can push updates, it filters
     * the data provider's items to find the book with the specified ID and
     * refreshes that item.
     *
     * @param id
     *            the ID of the product to refresh
     */
    public void refreshProductAsync(Integer id) {
        Utils.access(ui, () -> {
            if (canPush()) {
                refreshProductById(id);
            }
        });
    }

    private void refreshProductById(Integer id) {
        var item = grid.findProductById(id);
        if (item != null) {
            grid.refresh(item);
        }
    }

    private boolean canPush() {
        return isAttached() && grid.hasDataProvider();
    }

    @Nullable
    private Product updateProductInGrid(Product product) {
        assert product != null : "Product cannot be null";
        var id = product.getId();
        assert id != null : "Product ID cannot be null";

        var updatedProduct = presenter.findProduct(id);
        if (updatedProduct != null) {
            grid.replaceProduct(product, updatedProduct);
            logger.debug("Refreshed {}", id);
        } else {
            grid.removeProduct(product);
        }
        return updatedProduct;
    }

}