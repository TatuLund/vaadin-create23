package org.vaadin.tatu.vaadincreate.crud;

import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.tatu.vaadincreate.ConfirmDialog;
import org.vaadin.tatu.vaadincreate.ResetButtonForTextField;
import org.vaadin.tatu.vaadincreate.VaadinCreateTheme;
import org.vaadin.tatu.vaadincreate.VaadinCreateUI;
import org.vaadin.tatu.vaadincreate.auth.AccessControl;
import org.vaadin.tatu.vaadincreate.auth.RolesPermitted;
import org.vaadin.tatu.vaadincreate.backend.ProductDataService;
import org.vaadin.tatu.vaadincreate.backend.data.Product;
import org.vaadin.tatu.vaadincreate.backend.data.User.Role;
import org.vaadin.tatu.vaadincreate.eventbus.EventBus;
import org.vaadin.tatu.vaadincreate.eventbus.EventBus.EventBusListener;
import org.vaadin.tatu.vaadincreate.i18n.HasI18N;
import org.vaadin.tatu.vaadincreate.locking.LockedObjects.LockingEvent;

import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewBeforeLeaveEvent;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.Grid.SelectionMode;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.UIDetachedException;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

/**
 * A view for performing create-read-update-delete operations on products.
 *
 * See also {@link BookPresenter} for fetching the data, the actual CRUD
 * operations and controlling the view based on events from outside.
 */
@SuppressWarnings("serial")
@RolesPermitted({ Role.USER, Role.ADMIN })
public class BooksView extends CssLayout
        implements View, HasI18N, EventBusListener {

    public static final String VIEW_NAME = "inventory";

    private static final String UPDATED = "updated";
    private static final String REMOVED = "removed";
    private static final String CONFIRM = "confirm";
    private static final String NEW_PRODUCT = "new-product";
    private static final String FILTER = "filter";
    private static final String NOT_VALID_PID = "not-valid-pid";
    private static final String UNSAVED_CHANGES = "unsaved-changes";
    private static final String CANCEL = "cancel";

    private BookGrid grid;
    private BookForm form;
    private TextField filter;

    private BooksPresenter presenter = new BooksPresenter(this);
    private AccessControl accessControl = VaadinCreateUI.get()
            .getAccessControl();
    private EventBus eventBus = EventBus.get();

    private Button newProduct;

    private ListDataProvider<Product> dataProvider;
    private FakeGrid fakeGrid;
    private String params;

    public BooksView() {
        setSizeFull();
        addStyleName(VaadinCreateTheme.BOOKVIEW);
        var topLayout = createTopBar();

        grid = new BookGrid();
        grid.asSingleSelect().addValueChangeListener(event -> {
            if (form.hasChanges()) {
                var dialog = createDiscardChangesConfirmDialog();
                dialog.open();
                dialog.addConfirmedListener(e -> {
                    presenter.unlockBook();
                    form.showForm(false);
                    setFragmentParameter("");
                });
            } else {
                presenter.rowSelected(event.getValue());
            }
        });
        grid.setVisible(false);

        // Display fake Grid while loading data
        fakeGrid = new FakeGrid();

        form = new BookForm(presenter);

        var barAndGridLayout = new VerticalLayout();
        barAndGridLayout.addComponent(topLayout);
        barAndGridLayout.addComponent(fakeGrid);
        barAndGridLayout.addComponent(grid);
        barAndGridLayout.setSizeFull();
        barAndGridLayout.setExpandRatio(grid, 1);
        barAndGridLayout.setExpandRatio(fakeGrid, 1);
        barAndGridLayout.setStyleName(VaadinCreateTheme.BOOKVIEW_GRID);

        addComponent(barAndGridLayout);
        addComponent(form);

        eventBus.registerEventBusListener(this);
        presenter.init();
    }

    private boolean filterCondition(Object object, String filterText) {
        return object != null && object.toString().toLowerCase()
                .contains(filterText.toLowerCase());
    }

    private boolean passesFilter(Product book, String filterText) {
        filterText = filterText.trim();
        return filterCondition(book.getProductName(), filterText)
                || filterCondition(book.getAvailability(), filterText)
                || filterCondition(book.getCategory(), filterText);
    }

    private HorizontalLayout createTopBar() {
        filter = new TextField();
        filter.setId("filter-field");
        filter.setStyleName(VaadinCreateTheme.BOOKVIEW_FILTER);
        filter.addStyleName(ValoTheme.TEXTFIELD_INLINE_ICON);
        filter.setPlaceholder(getTranslation(FILTER));
        filter.setIcon(VaadinIcons.SEARCH);
        ResetButtonForTextField.extend(filter);
        // Apply the filter to grid's data provider. TextField value is never
        // null
        filter.addValueChangeListener(event -> dataProvider
                .setFilter(book -> passesFilter(book, event.getValue())));

        newProduct = new Button(getTranslation(NEW_PRODUCT));
        newProduct.setId("new-product");
        newProduct.addStyleName(ValoTheme.BUTTON_PRIMARY);
        newProduct.setIcon(VaadinIcons.PLUS_CIRCLE);
        newProduct.addClickListener(click -> presenter.newProduct());

        var topLayout = new HorizontalLayout();
        topLayout.setWidth("100%");
        topLayout.addComponents(filter, newProduct);
        topLayout.setComponentAlignment(filter, Alignment.MIDDLE_LEFT);
        topLayout.setExpandRatio(filter, 1);
        topLayout.setStyleName(VaadinCreateTheme.BOOKVIEW_TOOLBAR);
        return topLayout;
    }

    @Override
    public void enter(ViewChangeEvent event) {
        params = event.getParameters();
        if (!accessControl.isUserInRole(Role.ADMIN)) {
            grid.setSelectionMode(SelectionMode.NONE);
        }
        presenter.requestUpdateProducts();
        form.setCategories(ProductDataService.get().getAllCategories());
    }

    public void cancelProduct() {
        if (form.hasChanges()) {
            var dialog = createDiscardChangesConfirmDialog();
            dialog.open();
            dialog.addConfirmedListener(e -> {
                form.showForm(false);
                clearSelection();
                setFragmentParameter("");
            });
        } else {
            form.showForm(false);
            clearSelection();
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
        try {
            getUI().access(() -> {
                logger.info("Updating products");
                dataProvider = new ListDataProvider<>(products);
                grid.setDataProvider(dataProvider);
                grid.setVisible(true);
                fakeGrid.setVisible(false);
                // Open form with url parameter based book
                presenter.enter(params);
            });
        } catch (UIDetachedException e) {
            logger.info("Browser was closed, updates not pushed");
        }
    }

    public void showError(String msg) {
        Notification.show(msg, Type.ERROR_MESSAGE);
    }

    public void showNotValidId(String productId) {
        showError(getTranslation(NOT_VALID_PID, productId));
    }

    public void showSaveNotification(String book) {
        Notification.show(getTranslation(UPDATED, book),
                Type.TRAY_NOTIFICATION);
    }

    public void showDeleteNotification(String book) {
        Notification.show(getTranslation(REMOVED, book),
                Type.TRAY_NOTIFICATION);
    }

    public void setNewProductEnabled(boolean enabled) {
        newProduct.setEnabled(enabled);
    }

    public void clearSelection() {
        grid.getSelectionModel().deselectAll();
    }

    public void selectRow(Product row) {
        grid.getSelectionModel().select(row);
    }

    public Product getSelectedRow() {
        return grid.getSelectedRow();
    }

    public void updateProduct(Product product) {
        logger.info("Refresh item");
        grid.setEdited(product);
        dataProvider.refreshItem(product);
        form.showForm(false);
    }

    public void updateGrid(Product product) {
        logger.info("Refresh grid");
        dataProvider.getItems().add(product);
        dataProvider.refreshAll();
        form.showForm(false);
        grid.scrollToEnd();
    }

    public void removeProduct(Product product) {
        dataProvider.getItems().remove(product);
        dataProvider.refreshAll();
    }

    public void editProduct(Product product) {
        grid.setEdited(null);
        if (product != null) {
            if (product.getId() > 0) {
                product = refreshProduct(dataProvider, product);
            }
            form.showForm(true);
        } else {
            form.showForm(false);
        }
        form.editProduct(product);
    }

    @Override
    public void detach() {
        super.detach();
        eventBus.unregisterEventBusListener(this);
        // If detach happens before completion of data fetch, cancel the fetch
        presenter.cancelUpdateProducts();
    }

    /**
     * Update the fragment without causing navigator to change view
     *
     * @param productId
     *            The parameter
     */
    public void setFragmentParameter(String productId) {
        String fragmentParameter;
        if (productId == null || productId.isEmpty()) {
            fragmentParameter = "";
        } else {
            fragmentParameter = productId;
        }

        var page = VaadinCreateUI.get().getPage();
        var path = "!" + BooksView.VIEW_NAME + "/" + fragmentParameter;
        page.setUriFragment(path, false);
    }

    @Override
    public void beforeLeave(ViewBeforeLeaveEvent event) {
        if (form.hasChanges()) {
            var dialog = createDiscardChangesConfirmDialog();
            dialog.open();
            dialog.addConfirmedListener(e -> {
                event.navigate();
            });
            // IMHO: Navigator clears url too early and this workaround
            // shouldn't be necessary. This is a possible bug.
            var book = getSelectedRow();
            if (book != null) {
                getUI().access(() -> {
                    logger.debug("Set fragment: " + book.getId());
                    setFragmentParameter("" + book.getId());
                });
            }
        } else {
            event.navigate();
        }
    }

    private ConfirmDialog createDiscardChangesConfirmDialog() {
        var dialog = new ConfirmDialog(getTranslation(UNSAVED_CHANGES),
                ConfirmDialog.Type.ALERT);
        dialog.setConfirmText(getTranslation(CONFIRM));
        dialog.setCancelText(getTranslation(CANCEL));
        return dialog;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void eventFired(Object event) {
        if (event instanceof LockingEvent && isAttached()) {
            var bookEvent = (LockingEvent) event;
            getUI().access(() -> {
                ListDataProvider<Product> dataProvider = (ListDataProvider<Product>) grid
                        .getDataProvider();
                dataProvider.getItems().stream()
                        .filter(book -> book.getId() == bookEvent.getId())
                        .findFirst().ifPresent(product -> {
                            dataProvider.refreshItem(product);
                        });
            });
        }
    }

    private Product refreshProduct(ListDataProvider<Product> dataProvider,
            Product product) {
        var list = ((List<Product>) dataProvider.getItems());
        var updatedProduct = presenter.findProduct(product.getId());
        list.set(list.indexOf(product), updatedProduct);
        logger.info("Refreshed {}", product.getId());
        dataProvider.refreshItem(updatedProduct);
        return updatedProduct;
    }

    private Logger logger = LoggerFactory.getLogger(this.getClass());

}