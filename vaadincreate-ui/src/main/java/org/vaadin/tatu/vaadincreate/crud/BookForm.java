package org.vaadin.tatu.vaadincreate.crud;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Collection;
import java.util.Locale;

import org.vaadin.tatu.vaadincreate.AttributeExtension;
import org.vaadin.tatu.vaadincreate.CharacterCountExtension;
import org.vaadin.tatu.vaadincreate.ConfirmDialog;
import org.vaadin.tatu.vaadincreate.ConfirmDialog.Type;
import org.vaadin.tatu.vaadincreate.auth.AccessControl;
import org.vaadin.tatu.vaadincreate.VaadinCreateTheme;
import org.vaadin.tatu.vaadincreate.VaadinCreateUI;
import org.vaadin.tatu.vaadincreate.backend.data.Availability;
import org.vaadin.tatu.vaadincreate.backend.data.Category;
import org.vaadin.tatu.vaadincreate.backend.data.Product;
import org.vaadin.tatu.vaadincreate.i18n.HasI18N;

import com.vaadin.data.BeanValidationBinder;
import com.vaadin.data.Binder;
import com.vaadin.data.Result;
import com.vaadin.data.ValueContext;
import com.vaadin.data.converter.StringToIntegerConverter;
import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.server.AbstractErrorMessage.ContentMode;
import com.vaadin.server.Page;
import com.vaadin.server.UserError;
import com.vaadin.shared.ui.ErrorLevel;
import com.vaadin.ui.AbstractComponent;
import com.vaadin.ui.Button;
import com.vaadin.ui.CheckBoxGroup;
import com.vaadin.ui.Component;
import com.vaadin.ui.Composite;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.JavaScript;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

@SuppressWarnings({ "serial", "java:S2160" })
public class BookForm extends Composite implements HasI18N {

    // Localization constants
    private static final String AVAILABILITY_MISMATCH = "availability-mismatch";
    private static final String SAVE = "save";
    private static final String CANCEL = "cancel";
    private static final String CANNOT_CONVERT = "cannot-convert";
    private static final String CATEGORIES = "categories";
    private static final String DISCARD = "discard";
    private static final String AVAILABILITY = "availability";
    private static final String IN_STOCK = "in-stock";
    private static final String PRICE = "price";
    private static final String PRODUCT_NAME = "product-name";
    private static final String DELETE = "delete";
    private static final String WILL_DELETE = "will-delete";

    protected TextField productName = new TextField(
            getTranslation(PRODUCT_NAME));
    protected TextField price = new TextField(getTranslation(PRICE));
    protected TextField stockCount = new TextField(getTranslation(IN_STOCK));
    protected AvailabilitySelector availability = new AvailabilitySelector(
            getTranslation(AVAILABILITY));
    protected CheckBoxGroup<Category> category = new CheckBoxGroup<>(
            getTranslation(CATEGORIES));

    protected Button saveButton = new Button(getTranslation(SAVE));
    protected Button discardButton = new Button(getTranslation(DISCARD));
    protected Button cancelButton = new Button(getTranslation(CANCEL));
    protected Button deleteButton = new Button(getTranslation(DELETE));

    private AccessControl accessControl = VaadinCreateUI.get()
            .getAccessControl();
    private Binder<Product> binder;
    private Product currentProduct;
    private CssLayout layout = new CssLayout();
    private BooksPresenter presenter;

    private static class StockCountConverter extends StringToIntegerConverter {

        public StockCountConverter(String message) {
            super(message);
        }

        @Override
        protected NumberFormat getFormat(Locale locale) {
            // do not use a thousands separator, as HTML5 input type
            // number expects a fixed wire/DOM number format regardless
            // of how the browser presents it to the user (which could
            // depend on the browser locale)
            var format = new DecimalFormat();
            format.setMaximumFractionDigits(0);
            format.setDecimalSeparatorAlwaysShown(false);
            format.setParseIntegerOnly(true);
            format.setGroupingUsed(false);
            return format;
        }

        @Override
        public Result<Integer> convertToModel(String value,
                ValueContext context) {
            Result<Integer> result = super.convertToModel(value, context);
            return result.map(stock -> stock == null ? 0 : stock);
        }

    }

    /**
     * Represents a form for creating or editing a book. This form is used in
     * the Vaadin Create application. It allows users to enter information about
     * a book, such as its price, stock count, and category. The form includes
     * validation for the entered data and provides buttons for saving,
     * discarding, canceling, and deleting the book. The form is bound to a
     * presenter, which handles the business logic for saving, editing, and
     * deleting books.
     */
    public BookForm(BooksPresenter presenter) {
        this.presenter = presenter;
        setCompositionRoot(layout);
        buildForm();
        layout.setId("book-form");

        binder = new BeanValidationBinder<>(Product.class);
        binder.forField(price)
                .withConverter(
                        new EuroConverter(getTranslation(CANNOT_CONVERT)))
                .bind("price");
        binder.forField(stockCount)
                .withConverter(
                        new StockCountConverter(getTranslation(CANNOT_CONVERT)))
                .bind("stockCount");

        category.setItemCaptionGenerator(Category::getName);
        binder.forField(category).bind("category");

        // Add bean level validation for Availability vs. Stock count cross
        // checking.
        binder.withValidator(this::checkAvailabilityVsStockCount, "Error");

        binder.bindInstanceFields(this);

        // enable/disable save button while editing
        binder.addStatusChangeListener(event -> {
            var isValid = !event.hasValidationErrors();
            var hasChanges = binder.hasChanges();
            saveButton.setEnabled(hasChanges && isValid);
            discardButton.setEnabled(hasChanges);
            if (isValid) {
                setStockCountAndAvailabilityInvalid(false);
            }
        });

        saveButton.addClickListener(event -> handleSave());

        discardButton.addClickListener(event -> {
            presenter.editProduct(currentProduct);
            updateDirtyIndicators();
        });

        cancelButton.addClickListener(event -> {
            presenter.cancelProduct();
        });
        cancelButton.setClickShortcut(KeyCode.ESCAPE);

        deleteButton.addClickListener(event -> handleDelete());
    }

    private void handleSave() {
        if (presenter.validateCategories(category.getValue())) {
            if (currentProduct != null
                    && binder.writeBeanIfValid(currentProduct)) {
                presenter.saveProduct(currentProduct);
            } else if (binderHasInvalidFieldsBound()) {
                setStockCountAndAvailabilityInvalid(true);
            }
        }
    }

    private void handleDelete() {
        if (currentProduct != null) {
            var dialog = new ConfirmDialog(getTranslation(WILL_DELETE,
                    currentProduct.getProductName()), Type.ALERT);
            dialog.setConfirmText(getTranslation(DELETE));
            dialog.setCancelText(getTranslation(CANCEL));
            dialog.open();
            dialog.addConfirmedListener(e -> {
                presenter.deleteProduct(currentProduct);
                showForm(false);
            });
        }
    }

    private boolean binderHasInvalidFieldsBound() {
        return binder.getFields().filter(field -> ((AbstractComponent) field)
                .getComponentError() != null).count() == 0;
    }

    // Bean level validation
    private boolean checkAvailabilityVsStockCount(Product product) {
        return (product.getAvailability() == Availability.AVAILABLE
                && product.getStockCount() > 0)
                || (product.getAvailability() == Availability.DISCONTINUED
                        && product.getStockCount() == 0)
                || (product.getAvailability() == Availability.COMING
                        && product.getStockCount() == 0);
    }

    // Set the stock count and availability fields as invalid
    private void setStockCountAndAvailabilityInvalid(boolean invalid) {
        if (invalid) {
            stockCount.setComponentError(
                    new UserError(getTranslation(AVAILABILITY_MISMATCH),
                            ContentMode.TEXT, ErrorLevel.ERROR));
            availability.setComponentError(
                    new UserError(getTranslation(AVAILABILITY_MISMATCH),
                            ContentMode.TEXT, ErrorLevel.ERROR));
        } else {
            stockCount.setComponentError(null);
            availability.setComponentError(null);
        }
    }

    public void showForm(boolean visible) {
        accessControl.assertAdmin();
        // This process is tricky. The element needs to be in DOM and not having
        // 'display: none' in order to CSS animations to work. We will set
        // display none after a delay so that pressing 'tab' key will not reveal
        // the form while set not visible.
        if (visible) {
            JavaScript.eval(
                    "document.getElementById('book-form').style.display='block';");
            clearDirtyIndicators();
            getUI().runAfterRoundTrip(() -> layout
                    .addStyleName(VaadinCreateTheme.BOOKFORM_WRAPPER_VISIBLE));
        } else {
            layout.removeStyleName(VaadinCreateTheme.BOOKFORM_WRAPPER_VISIBLE);
            if (isAttached()) {
                getUI().runAfterRoundTrip(() -> {
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    JavaScript.eval(
                            "document.getElementById('book-form').style.display='none';");
                });
            }
        }
        setEnabled(visible);
    }

    public Product getProduct() {
        return currentProduct;
    }

    public boolean isShown() {
        return layout.getStyleName()
                .contains(VaadinCreateTheme.BOOKFORM_WRAPPER_VISIBLE);
    }

    public boolean hasChanges() {
        if (!isShown()) {
            return false;
        }
        var hasChanges = binder.hasChanges();
        if (hasChanges) {
            updateDirtyIndicators();
        }
        return hasChanges;
    }

    public void updateDirtyIndicators() {
        clearDirtyIndicators();
        binder.getChangedBindings()
                .forEach(binding -> ((Component) binding.getField())
                        .addStyleName(VaadinCreateTheme.BOOKFORM_FIELD_DIRTY));
    }

    private void clearDirtyIndicators() {
        binder.getFields().forEach(field -> ((Component) field)
                .removeStyleName(VaadinCreateTheme.BOOKFORM_FIELD_DIRTY));
    }

    private void buildForm() {
        layout.addStyleNames(VaadinCreateTheme.BOOKFORM,
                VaadinCreateTheme.BOOKFORM_WRAPPER);
        var formLayout = new VerticalLayout();
        formLayout.setHeightFull();
        formLayout.setMargin(false);
        formLayout.addStyleName(VaadinCreateTheme.BOOKFORM_FORM);

        productName.setId("product-name");
        productName.setWidthFull();
        productName.setMaxLength(100);
        CharacterCountExtension.extend(productName);

        // Layout price and stockCount horizontally
        var fieldWrapper = new HorizontalLayout();
        fieldWrapper.setWidthFull();
        price.setId("price");
        price.setWidthFull();
        // Mark the stock count field as numeric.
        // This affects the virtual keyboard shown on mobile devices.
        var stockFieldExtension = new AttributeExtension();
        stockFieldExtension.extend(stockCount);
        stockFieldExtension.setAttribute("type", "number");
        stockCount.setId("stock-count");
        stockCount.setWidthFull();
        fieldWrapper.addComponents(price, stockCount);

        category.setId("category");
        category.setWidthFull();

        var spacer = new CssLayout();

        // Buttons
        saveButton.addStyleName(ValoTheme.BUTTON_PRIMARY);
        saveButton.setId("save-button");
        discardButton.setId("discard-button");
        cancelButton.addStyleName(VaadinCreateTheme.BUTTON_CANCEL);
        deleteButton.addStyleName(ValoTheme.BUTTON_DANGER);
        deleteButton.setId("delete-button");
        deleteButton.setEnabled(false);

        formLayout.addComponents(productName, fieldWrapper, availability,
                category);
        formLayout.addComponent(spacer);
        formLayout.addComponents(saveButton, discardButton, cancelButton,
                deleteButton);
        formLayout.setExpandRatio(spacer, 1);
        layout.addComponent(formLayout);
    }

    @SuppressWarnings("unchecked")
    public void setCategories(Collection<Category> categories) {
        category.setItems(categories);
        // Show selected items first in the list
        var dataProvider = (ListDataProvider<Category>) category
                .getDataProvider();
        dataProvider.setSortComparator(
                (a, b) -> category.getValue().contains(a) ? -1 : 1);
        if (getProduct() != null) {
            category.setValue(getProduct().getCategory());
        }
    }

    public void editProduct(Product product) {
        accessControl.assertAdmin();
        presenter.requestUpdateCategories();
        if (product == null) {
            product = new Product();
        }
        deleteButton.setEnabled(product.getId() != -1);
        currentProduct = product;
        binder.readBean(product);

        // Scroll to the top
        // As this is not a Panel, using JavaScript
        var scrollScript = "window.document.getElementById('" + getId()
                + "').scrollTop = 0;";
        Page.getCurrent().getJavaScript().execute(scrollScript);
    }
}
