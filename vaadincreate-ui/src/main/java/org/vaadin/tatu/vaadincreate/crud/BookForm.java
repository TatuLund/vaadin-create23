package org.vaadin.tatu.vaadincreate.crud;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Collection;
import java.util.Locale;

import org.vaadin.tatu.vaadincreate.AttributeExtension;
import org.vaadin.tatu.vaadincreate.CharacterCountExtension;
import org.vaadin.tatu.vaadincreate.ConfirmDialog;
import org.vaadin.tatu.vaadincreate.ConfirmDialog.Type;
import org.vaadin.tatu.vaadincreate.VaadinCreateTheme;
import org.vaadin.tatu.vaadincreate.backend.data.Availability;
import org.vaadin.tatu.vaadincreate.backend.data.Category;
import org.vaadin.tatu.vaadincreate.backend.data.Product;
import org.vaadin.tatu.vaadincreate.i18n.HasI18N;

import com.vaadin.data.BeanValidationBinder;
import com.vaadin.data.Binder;
import com.vaadin.data.Result;
import com.vaadin.data.ValueContext;
import com.vaadin.data.converter.StringToIntegerConverter;
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
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

@SuppressWarnings("serial")
public class BookForm extends Composite implements HasI18N {

    // Localization constants
    private static final String AVAILABILITY_MISMATCH = "availability-mismatch";
    private static final String DISCARD_CHANGES = "discard-changes";
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
    protected Button save = new Button(getTranslation(SAVE));
    protected Button discard = new Button(getTranslation(DISCARD));
    protected Button cancel = new Button(getTranslation(CANCEL));
    protected Button delete = new Button(getTranslation(DELETE));

    private Binder<Product> binder;
    private Product currentProduct;
    private CssLayout layout = new CssLayout();

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

    public BookForm(BooksPresenter presenter) {
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
            save.setEnabled(hasChanges && isValid);
            discard.setEnabled(hasChanges);
            if (isValid) {
                flagStockCountAndAvailabilityInvalid(false);
            }
        });

        save.addClickListener(event -> {
            if (currentProduct != null
                    && binder.writeBeanIfValid(currentProduct)) {
                presenter.saveProduct(currentProduct);
            } else if (binderHasInvalidFieldsBound()) {
                flagStockCountAndAvailabilityInvalid(true);
            }
        });

        discard.addClickListener(event -> {
            presenter.editProduct(currentProduct);
            updateDirtyIndicators();
        });

        cancel.addClickListener(event -> {
            presenter.cancelProduct();
        });

        delete.addClickListener(event -> {
            if (currentProduct != null) {
                var dialog = new ConfirmDialog(getTranslation(WILL_DELETE,
                        currentProduct.getProductName()), Type.ALERT);
                dialog.setConfirmText(getTranslation(DELETE));
                dialog.setCancelText(getTranslation(CANCEL));
                dialog.open();
                dialog.addConfirmedListener(e -> {
                    presenter.deleteProduct(currentProduct);
                });
            }
        });
    }

    private boolean binderHasInvalidFieldsBound() {
        return binder.getFields().filter(field -> ((AbstractComponent) field)
                .getComponentError() != null).count() == 0;
    }

    private boolean checkAvailabilityVsStockCount(Product product) {
        return (product.getAvailability() == Availability.AVAILABLE
                && product.getStockCount() > 0)
                || (product.getAvailability() == Availability.DISCONTINUED
                        && product.getStockCount() == 0)
                || (product.getAvailability() == Availability.COMING
                        && product.getStockCount() == 0);
    }

    private void flagStockCountAndAvailabilityInvalid(boolean invalid) {
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
        if (visible) {
            updateDirtyIndicators();
            layout.addStyleName(VaadinCreateTheme.BOOKFORM_WRAPPER_VISIBLE);
        } else {
            layout.removeStyleName(VaadinCreateTheme.BOOKFORM_WRAPPER_VISIBLE);
        }
        setEnabled(visible);
    }

    public boolean isShown() {
        var isShown = layout.getStyleName()
                .contains(VaadinCreateTheme.BOOKFORM_WRAPPER_VISIBLE);
        return isShown;
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
        binder.getFields().forEach(field -> ((Component) field)
                .removeStyleName(VaadinCreateTheme.BOOKFORM_FIELD_DIRTY));
        binder.getChangedBindings()
                .forEach(binding -> ((Component) binding.getField())
                        .addStyleName(VaadinCreateTheme.BOOKFORM_FIELD_DIRTY));
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
        save.addStyleName(ValoTheme.BUTTON_PRIMARY);
        save.setId("save-button");
        discard.addStyleName("cancel");
        discard.setId("discard-button");
        cancel.addStyleName("cancel");
        delete.addStyleName(ValoTheme.BUTTON_DANGER);
        delete.setId("delete-button");
        delete.setEnabled(false);

        formLayout.addComponents(productName, fieldWrapper, availability,
                category);
        formLayout.addComponent(spacer);
        formLayout.addComponents(save, discard, cancel, delete);
        formLayout.setExpandRatio(spacer, 1);
        layout.addComponent(formLayout);
    }

    public void setCategories(Collection<Category> categories) {
        category.setItems(categories);
    }

    public void editProduct(Product product) {
        if (product == null) {
            product = new Product();
        }
        delete.setEnabled(product.getId() != -1);
        currentProduct = product;
        binder.readBean(product);

        // Scroll to the top
        // As this is not a Panel, using JavaScript
        var scrollScript = "window.document.getElementById('" + getId()
                + "').scrollTop = 0;";
        Page.getCurrent().getJavaScript().execute(scrollScript);
    }

    @Override
    public void attach() {
        super.attach();
    }
}
