package org.vaadin.tatu.vaadincreate.crud;

import org.vaadin.tatu.vaadincreate.backend.ProductDataService;
import org.vaadin.tatu.vaadincreate.backend.data.Product;
import org.vaadin.tatu.vaadincreate.backend.data.User.Role;

import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.tatu.vaadincreate.ConfirmDialog;
import org.vaadin.tatu.vaadincreate.ResetButtonForTextField;
import org.vaadin.tatu.vaadincreate.VaadinCreateTheme;
import org.vaadin.tatu.vaadincreate.auth.RolesPermitted;

import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.CssLayout;
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
public class BooksView extends CssLayout implements View {

    public static final String VIEW_NAME = "books";
    private BookGrid grid;
    private BookForm form;
    private TextField filter;

    private BooksPresenter presenter = new BooksPresenter(this);
    private Button newProduct;

    private ListDataProvider<Product> dataProvider;
    private UI ui;
    private VerticalLayout fakeGrid;
    private String params;

    public BooksView() {
        setSizeFull();
        addStyleName(VaadinCreateTheme.BOOKVIEW);
        var topLayout = createTopBar();

        grid = new BookGrid();
        grid.asSingleSelect().addValueChangeListener(event -> {
            if (form.hasChanges()) {
                var dialog = new ConfirmDialog(
                        "There are unsaved changes. Are you sure to change the book?",
                        ConfirmDialog.Type.ALERT);
                getUI().addWindow(dialog);
                dialog.addConfirmedListener(e -> {
                    presenter.rowSelected(event.getValue());
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

        presenter.init();
    }

    private boolean filterCondition(Object object, String filterText) {
        return object != null
                && object.toString().toLowerCase().contains(filterText);
    }

    private boolean passesFilter(Product book, String filterText) {
        filterText = filterText.trim();
        return filterCondition(book.getProductName(), filterText)
                || filterCondition(book.getAvailability(), filterText)
                || filterCondition(book.getCategory(), filterText);
    }

    public HorizontalLayout createTopBar() {
        filter = new TextField();
        filter.setId("filter-field");
        filter.setStyleName(VaadinCreateTheme.BOOKVIEW_FILTER);
        filter.setPlaceholder("Filter name, availability or category");
        ResetButtonForTextField.extend(filter);
        // Apply the filter to grid's data provider. TextField value is never
        // null
        filter.addValueChangeListener(event -> dataProvider
                .setFilter(book -> passesFilter(book, event.getValue())));

        newProduct = new Button("New product");
        newProduct.setId("new-product");
        newProduct.addStyleName(ValoTheme.BUTTON_PRIMARY);
        newProduct.setIcon(VaadinIcons.PLUS_CIRCLE);
        newProduct.addClickListener(click -> presenter.newProduct());

        var topLayout = new HorizontalLayout();
        topLayout.setWidth("100%");
        topLayout.addComponent(filter);
        topLayout.addComponent(newProduct);
        topLayout.setComponentAlignment(filter, Alignment.MIDDLE_LEFT);
        topLayout.setExpandRatio(filter, 1);
        topLayout.setStyleName(VaadinCreateTheme.BOOKVIEW_TOOLBAR);
        return topLayout;
    }

    @Override
    public void enter(ViewChangeEvent event) {
        ui = UI.getCurrent();
        params = event.getParameters();
        presenter.requestUpdateProducts();
        form.setCategories(ProductDataService.get().getAllCategories());
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
            ui.access(() -> {
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

    public void showSaveNotification(String msg) {
        Notification.show(msg, Type.TRAY_NOTIFICATION);
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
        var newProduct = product.getId() == -1;
        grid.setEdited(product);
        if (newProduct) {
            dataProvider.refreshAll();
            form.showForm(false);
            grid.scrollToEnd();
        } else {
            dataProvider.refreshItem(product);
            form.showForm(false);
        }
    }

    public void removeProduct(Product product) {
        dataProvider.refreshAll();
    }

    public void editProduct(Product product) {
        grid.setEdited(null);
        if (product != null) {
            form.showForm(true);
        } else {
            form.showForm(false);
        }
        form.editProduct(product);
    }

    @Override
    public void detach() {
        super.detach();
        // If detach happens before completion of data fetch, cancel the fetch
        presenter.cancelUpdateProducts();
    }

    private Logger logger = LoggerFactory.getLogger(this.getClass());
}