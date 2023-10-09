package org.vaadin.tatu.vaadincreate.crud;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Collection;
import java.util.Locale;

import org.vaadin.tatu.vaadincreate.AttributeExtension;
import org.vaadin.tatu.vaadincreate.CharacterCountExtension;
import org.vaadin.tatu.vaadincreate.VaadinCreateTheme;
import org.vaadin.tatu.vaadincreate.backend.data.Category;
import org.vaadin.tatu.vaadincreate.backend.data.Product;

import com.vaadin.data.BeanValidationBinder;
import com.vaadin.data.Binder;
import com.vaadin.data.Result;
import com.vaadin.data.converter.StringToIntegerConverter;
import com.vaadin.data.ValueContext;
import com.vaadin.server.Page;
import com.vaadin.ui.Button;
import com.vaadin.ui.CheckBoxGroup;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

@SuppressWarnings("serial")
public class BookForm extends CssLayout {

    protected TextField productName = new TextField("Product name");
    protected TextField price = new TextField("Price");
    protected TextField stockCount = new TextField("In stock");
    protected AvailabilitySelector availability = new AvailabilitySelector(
            "Availability");
    protected CheckBoxGroup<Category> category = new CheckBoxGroup<>(
            "Categories");
    protected Button save = new Button("Save");
    protected Button discard = new Button("Discard changes");
    protected Button cancel = new Button("Cancel");
    protected Button delete = new Button("Delete");

    private Binder<Product> binder;
    private Product currentProduct;

    private static class StockPriceConverter extends StringToIntegerConverter {

        public StockPriceConverter() {
            super("Could not convert value to " + Integer.class.getName());
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
        buildForm();
        setId("book-form");

        binder = new BeanValidationBinder<>(Product.class);
        binder.forField(price).withConverter(new EuroConverter()).bind("price");
        binder.forField(stockCount).withConverter(new StockPriceConverter())
                .bind("stockCount");

        category.setItemCaptionGenerator(Category::getName);
        binder.forField(category).bind("category");
        binder.bindInstanceFields(this);

        // enable/disable save button while editing
        binder.addStatusChangeListener(event -> {
            var isValid = !event.hasValidationErrors();
            var hasChanges = binder.hasChanges();
            save.setEnabled(hasChanges && isValid);
            discard.setEnabled(hasChanges);
        });

        save.addClickListener(event -> {
            if (currentProduct != null
                    && binder.writeBeanIfValid(currentProduct)) {
                presenter.saveProduct(currentProduct);
            }
        });

        discard.addClickListener(
                event -> presenter.editProduct(currentProduct));

        cancel.addClickListener(event -> {
            presenter.cancelProduct();
            removeStyleName(VaadinCreateTheme.BOOKFORM_WRAPPER_VISIBLE);
        });

        delete.addClickListener(event -> {
            if (currentProduct != null) {
                presenter.deleteProduct(currentProduct);
            }
        });
    }

    private void buildForm() {
        addStyleNames(VaadinCreateTheme.BOOKFORM,
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
        cancel.addStyleName("cancel");
        delete.addStyleName(ValoTheme.BUTTON_DANGER);
        delete.setId("delete-button");
        delete.setEnabled(false);

        formLayout.addComponents(productName, fieldWrapper, availability,
                category);
        formLayout.addComponent(spacer);
        formLayout.addComponents(save, discard, cancel, delete);
        formLayout.setExpandRatio(spacer, 1);
        addComponent(formLayout);
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
