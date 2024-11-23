package org.vaadin.tatu.vaadincreate.crud.form;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.tatu.vaadincreate.AttributeExtension;
import org.vaadin.tatu.vaadincreate.CharacterCountExtension;
import org.vaadin.tatu.vaadincreate.ConfirmDialog;
import org.vaadin.tatu.vaadincreate.ConfirmDialog.Type;
import org.vaadin.tatu.vaadincreate.VaadinCreateTheme;
import org.vaadin.tatu.vaadincreate.VaadinCreateUI;
import org.vaadin.tatu.vaadincreate.auth.AccessControl;
import org.vaadin.tatu.vaadincreate.backend.data.Availability;
import org.vaadin.tatu.vaadincreate.backend.data.Category;
import org.vaadin.tatu.vaadincreate.backend.data.Product;
import org.vaadin.tatu.vaadincreate.crud.BookGrid;
import org.vaadin.tatu.vaadincreate.crud.BooksPresenter;
import org.vaadin.tatu.vaadincreate.crud.EuroConverter;
import org.vaadin.tatu.vaadincreate.i18n.HasI18N;
import org.vaadin.tatu.vaadincreate.i18n.I18n;
import org.vaadin.tatu.vaadincreate.util.Utils;

import com.vaadin.data.BeanValidationBinder;
import com.vaadin.data.Binder;
import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.event.ShortcutListener;
import com.vaadin.server.AbstractErrorMessage;
import com.vaadin.server.Page;
import com.vaadin.server.UserError;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.shared.ui.ErrorLevel;
import com.vaadin.ui.AbstractComponent;
import com.vaadin.ui.Button;
import com.vaadin.ui.CheckBoxGroup;
import com.vaadin.ui.Composite;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

/**
 * Represents a form for creating or editing a book. This form is used in the
 * Vaadin Create application. It allows users to enter information about a book,
 * such as its price, stock count, and category. The form includes validation
 * for the entered data and provides buttons for saving, discarding, canceling,
 * and deleting the book. The form is bound to a presenter, which handles the
 * business logic for saving, editing, and deleting books.
 *
 * <p>
 * The form fields are bound to the product object by naming convention. E.g.
 * using the field name "productName" will bind to the Product's "productName"
 * property.
 * </p>
 *
 * <p>
 * The form includes the following fields:
 * <ul>
 * <li>productName - TextField for the product name</li>
 * <li>price - TextField for the product price</li>
 * <li>stockCount - NumberField for the stock count</li>
 * <li>availability - AvailabilitySelector for the product availability</li>
 * <li>category - CheckBoxGroup for the product categories</li>
 * </ul>
 * </p>
 *
 * <p>
 * The form includes the following buttons:
 * <ul>
 * <li>saveButton - Button for saving the product</li>
 * <li>discardButton - Button for discarding changes</li>
 * <li>cancelButton - Button for canceling the operation</li>
 * <li>deleteButton - Button for deleting the product</li>
 * </ul>
 * </p>
 *
 * <p>
 * The form uses a Binder for binding the fields to the Product object and
 * includes validation for the fields. It also includes bean level validation
 * for checking the availability vs. stock count.
 * </p>
 *
 * <p>
 * The form includes methods for handling save, delete, and other operations, as
 * well as methods for showing/hiding the form, checking for changes, and
 * updating dirty indicators.
 * </p>
 *
 * <p>
 * The form also includes methods for setting categories, editing a product,
 * merging a draft product, and focusing on the product name field.
 * </p>
 *
 * <p>
 * The form is designed to be used with a presenter, which handles the business
 * logic for saving, editing, and deleting products.
 * </p>
 */
@SuppressWarnings({ "serial", "java:S2160" })
public class BookForm extends Composite implements HasI18N {

    // The form fields are bound to the product object by naming convention.
    // E.g. using the field name "productName" will bind to the Product's
    // "productName" property.
    protected TextField productName = new TextField(
            getTranslation(I18n.PRODUCT_NAME));
    protected TextField price = new TextField(getTranslation(I18n.PRICE));
    protected NumberField stockCount = new NumberField(
            getTranslation(I18n.IN_STOCK));
    protected AvailabilitySelector availability = new AvailabilitySelector(
            getTranslation(I18n.AVAILABILITY));
    protected CheckBoxGroup<Category> category = new CheckBoxGroup<>(
            getTranslation(I18n.CATEGORIES));

    protected Button saveButton = new Button(getTranslation(I18n.SAVE));
    protected Button discardButton = new Button(getTranslation(I18n.DISCARD));
    protected Button cancelButton = new Button(getTranslation(I18n.CANCEL));
    protected Button deleteButton = new Button(getTranslation(I18n.DELETE));
    private Label selectionLabel = new Label();

    private AccessControl accessControl = VaadinCreateUI.get()
            .getAccessControl();
    private Binder<Product> binder;
    private Product currentProduct;
    private SidePanel sidePanel = new SidePanel();
    private BooksPresenter presenter;
    private boolean visible;
    private boolean isValid;

    /**
     * Creates a new BookForm with the given presenter.
     *
     * @param presenter
     *            the presenter for the form.
     * @param grid
     */
    public BookForm(BooksPresenter presenter, BookGrid grid) {
        this.presenter = presenter;
        setCompositionRoot(sidePanel);
        buildForm();

        selectionLabel.setContentMode(ContentMode.HTML);
        selectionLabel.setValue("<div aria-live='polite' role='alert'></div>");

        binder = new BeanValidationBinder<>(Product.class);
        binder.forField(price)
                .withConverter(new EuroConverter(
                        getTranslation(I18n.Form.CANNOT_CONVERT)))
                .bind("price");
        binder.forField(stockCount).bind("stockCount");

        category.setItemCaptionGenerator(Category::getName);
        binder.forField(category).bind("category");

        // Add bean level validation for Availability vs. Stock count cross
        // checking.
        binder.withValidator(this::checkAvailabilityVsStockCount, "Error");

        binder.bindInstanceFields(this);
        binder.setChangeDetectionEnabled(true);

        // enable/disable save button while editing
        binder.addStatusChangeListener(event -> {
            isValid = !event.hasValidationErrors();
            if (isValid) {
                setStockCountAndAvailabilityInvalid(false);
            }
            if (!isValid) {
                saveButton.setEnabled(false);
            }
        });

        binder.addValueChangeListener(event -> {
            var hasChanges = binder.hasChanges();
            saveButton.setEnabled(hasChanges && isValid);
            discardButton.setEnabled(hasChanges);
        });

        saveButton.addClickListener(event -> handleSave());

        discardButton.addClickListener(event -> {
            presenter.editProduct(currentProduct);
            updateDirtyIndicators();
        });

        cancelButton.addClickListener(event -> presenter.cancelProduct());
        cancelButton.setClickShortcut(KeyCode.ESCAPE);

        deleteButton.addClickListener(event -> handleDelete());
        addShortcutListener(
                new ShortcutListener("Next", KeyCode.PAGE_DOWN, null) {
                    @Override
                    public void handleAction(Object sender, Object target) {
                        selectNextProduct(presenter, grid);
                    }
                });
        addShortcutListener(
                new ShortcutListener("Previous", KeyCode.PAGE_UP, null) {
                    @Override
                    public void handleAction(Object sender, Object target) {
                        selectPreviousProduct(presenter, grid);
                    }
                });
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
            var dialog = new ConfirmDialog(getTranslation(I18n.WILL_DELETE,
                    currentProduct.getProductName()), Type.ALERT);
            dialog.setConfirmText(getTranslation(I18n.DELETE));
            dialog.setCancelText(getTranslation(I18n.CANCEL));
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
            stockCount.setComponentError(new UserError(
                    getTranslation(I18n.Form.AVAILABILITY_MISMATCH),
                    AbstractErrorMessage.ContentMode.TEXT, ErrorLevel.ERROR));
            availability.setComponentError(new UserError(
                    getTranslation(I18n.Form.AVAILABILITY_MISMATCH),
                    AbstractErrorMessage.ContentMode.TEXT, ErrorLevel.ERROR));
        } else {
            stockCount.setComponentError(null);
            availability.setComponentError(null);
        }
    }

    /**
     * Slide the form in/out. True slides the form in, false slides out.
     *
     * @param visible
     *            boolean value.
     */
    public void showForm(boolean visible) {
        accessControl.assertAdmin();
        this.visible = visible;
        if (visible) {
            clearDirtyIndicators();
        }
        sidePanel.show(visible);
    }

    public Product getProduct() {
        return currentProduct;
    }

    public boolean isShown() {
        return visible;
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

    private void updateDirtyIndicators() {
        clearDirtyIndicators();
        binder.getChangedBindings().forEach(binding -> {
            var field = ((AbstractComponent) binding.getField());
            field.addStyleName(VaadinCreateTheme.BOOKFORM_FIELD_DIRTY);
            var value = binding.getGetter().apply(currentProduct);
            if (value != null) {
                field.setDescription(Utils.sanitize(String.format(
                        "<b>%s:</b><br>%s", getTranslation(I18n.Form.WAS),
                        convertValue(value))), ContentMode.HTML);
            }
        });
    }

    private static <T> String convertValue(T value) {
        if (value == null) {
            return "";
        }
        if (value instanceof BigDecimal) {
            var euroConverter = new EuroConverter("");
            return Utils.convertToPresentation((BigDecimal) value,
                    euroConverter);
        }
        if (value instanceof Availability) {
            return String.format("%s<span style='margin-right: 5px'>%s</span>",
                    Utils.createAvailabilityIcon((Availability) value),
                    value.toString());
        }
        return value.toString();
    }

    private void clearDirtyIndicators() {
        binder.getFields().forEach(hasValue -> {
            var field = (AbstractComponent) hasValue;
            field.removeStyleName(VaadinCreateTheme.BOOKFORM_FIELD_DIRTY);
            field.setDescription(null);
        });
    }

    private void buildForm() {
        var formLayout = new VerticalLayout();
        formLayout.setHeightFull();
        formLayout.setMargin(false);
        formLayout.addStyleName(VaadinCreateTheme.BOOKFORM_FORM);

        productName.setId("product-name");
        productName.setWidthFull();
        productName.setMaxLength(100);
        AttributeExtension.of(productName).setAttribute("autocomplete", "off");
        CharacterCountExtension.extend(productName);

        // Layout price and stockCount horizontally
        var fieldWrapper = new HorizontalLayout();
        fieldWrapper.setWidthFull();
        price.setId("price");
        price.setWidthFull();
        stockCount.setId("stock-count");
        stockCount.setWidthFull();
        fieldWrapper.addComponents(price, stockCount);

        category.setId("category");
        category.addStyleName(VaadinCreateTheme.CHECKBOXGROUP_SCROLL);
        category.setHeight("170px");
        category.setWidthFull();

        // Buttons
        saveButton.addStyleName(ValoTheme.BUTTON_PRIMARY);
        saveButton.setId("save-button");
        discardButton.setId("discard-button");
        cancelButton.addStyleName(VaadinCreateTheme.BUTTON_CANCEL);
        cancelButton.setId("cancel-button");
        deleteButton.addStyleName(ValoTheme.BUTTON_DANGER);
        deleteButton.setId("delete-button");
        deleteButton.setEnabled(false);

        formLayout.addComponents(productName, fieldWrapper, availability,
                category);
        formLayout.addComponents(selectionLabel);
        formLayout.addComponents(saveButton, discardButton, cancelButton,
                deleteButton);
        formLayout.setExpandRatio(selectionLabel, 1);

        // Set ARIA attributes for the form to make it accessible
        var attributes = AttributeExtension.of(formLayout);
        attributes.setAttribute("tabindex", "0");
        attributes.setAttribute("aria-label",
                getTranslation(I18n.Books.PRODUCT_FORM));
        attributes.setAttribute("role", "form");
        attributes.setAttribute("aria-keyshortcuts", "Escape PageDown PageUp");

        sidePanel.setContent(formLayout);
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
        deleteButton.setEnabled(product.getId() != null);
        currentProduct = product;
        binder.readBean(product);
        saveButton.setEnabled(false);
        discardButton.setEnabled(false);

        // Scroll to the top
        // As this is not a Panel, using JavaScript
        if (isAttached()) {
            var scrollScript = String.format(
                    "window.document.getElementById('%s').scrollTop = 0;",
                    getId());
            Page.getCurrent().getJavaScript().execute(scrollScript);
        }

        // Set ARIA attributes for the form to make it accessible
        selectionLabel.setValue(String.format(
                "<div role='alert' aria-live='polite' aria-label='%s %s'></div>",
                product.getProductName(), getTranslation(I18n.Books.OPENED)));

    }

    @Override
    public void detach() {
        super.detach();
        if (isShown() && binder.hasChanges()) {
            logger.info(
                    "Browser closed before saving changes, draft product autosaved.");
            var draft = new Product(getProduct());
            binder.writeBeanAsDraft(draft, true);
            presenter.saveDraft(draft);
        }
    }

    /**
     * Merge the draft product into the form.
     *
     * @param draft
     *            the draft product.
     */
    public void mergeDraft(Product draft) {
        // Binder does not support merging, so we need to do it manually
        var euroConverter = new EuroConverter("");

        var euros = Utils.convertToPresentation(draft.getPrice(),
                euroConverter);

        Utils.setValueIfDifferent(productName, draft.getProductName());
        Utils.setValueIfDifferent(stockCount, draft.getStockCount());
        Utils.setValueIfDifferent(category, draft.getCategory());
        Utils.setValueIfDifferent(availability, draft.getAvailability());
        Utils.setValueIfDifferent(price, euros);

        updateDirtyIndicators();
    }

    @Override
    public void focus() {
        productName.focus();
    }

    private static List<Product> getVisibleItems(BookGrid grid) {
        return grid.getDataCommunicator().fetchItemsWithRange(0,
                grid.getDataCommunicator().getDataProviderSize());
    }

    private void selectPreviousProduct(BooksPresenter presenter,
            BookGrid grid) {
        if (getProduct().getId() == null) {
            return;
        }
        var items = getVisibleItems(grid);
        var current = items.indexOf(getProduct());
        if (current > 0) {
            presenter.selectProduct(items.get(current - 1));
        }
    }

    private void selectNextProduct(BooksPresenter presenter, BookGrid grid) {
        if (getProduct().getId() == null) {
            return;
        }
        var items = getVisibleItems(grid);
        var current = items.indexOf(getProduct());
        if (current < items.size() - 1) {
            presenter.selectProduct(items.get(current + 1));
        }
    }

    private static Logger logger = LoggerFactory.getLogger(BookForm.class);
}
