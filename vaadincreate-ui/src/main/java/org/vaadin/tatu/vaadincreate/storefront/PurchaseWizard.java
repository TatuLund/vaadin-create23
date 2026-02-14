package org.vaadin.tatu.vaadincreate.storefront;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.tatu.vaadincreate.VaadinCreateTheme;
import org.vaadin.tatu.vaadincreate.auth.CurrentUser;
import org.vaadin.tatu.vaadincreate.backend.data.Address;
import org.vaadin.tatu.vaadincreate.backend.data.Cart;
import org.vaadin.tatu.vaadincreate.backend.data.User;
import org.vaadin.tatu.vaadincreate.crud.EuroConverter;
import org.vaadin.tatu.vaadincreate.crud.form.NumberField;
import org.vaadin.tatu.vaadincreate.i18n.HasI18N;
import org.vaadin.tatu.vaadincreate.i18n.I18n;

import com.vaadin.data.Binder;
import com.vaadin.data.ValidationException;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Composite;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Grid.SelectionMode;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

/**
 * Multi-step wizard for creating purchase requests. Guides the user through
 * product selection, address entry, supervisor selection, and final review.
 */
@NullMarked
@SuppressWarnings({ "serial", "java:S2160" })
public class PurchaseWizard extends Composite implements HasI18N {

    private static final Logger logger = LoggerFactory
            .getLogger(PurchaseWizard.class);

    private final Cart cart = new Cart();
    private final Address address = new Address();
    @Nullable
    private User selectedSupervisor;

    private int currentStep = 1;

    private final VerticalLayout root;
    private final Label stepTitle;
    private final VerticalLayout stepContent;
    private final Button nextButton;
    private final Button prevButton;

    private final StorefrontPresenter presenter = new StorefrontPresenter();

    // Step 1 components
    @Nullable
    private Grid<ProductDto> productGrid;
    private final Map<Integer, NumberField> quantityFields = new HashMap<>();

    // Step 2 components
    @Nullable
    private Binder<Address> addressBinder;
    @Nullable
    private TextField streetField;
    @Nullable
    private TextField postalCodeField;
    @Nullable
    private TextField cityField;
    @Nullable
    private TextField countryField;

    // Step 3 components
    @Nullable
    private ComboBox<User> supervisorComboBox;

    // Step 4 components
    @Nullable
    private Label reviewLabel;

    public PurchaseWizard() {
        root = new VerticalLayout();
        root.setMargin(true);
        root.setSpacing(true);
        root.setSizeFull();
        root.addStyleName(VaadinCreateTheme.STOREFRONTVIEW_WIZARD);

        stepTitle = new Label();
        stepTitle.addStyleName(ValoTheme.LABEL_H2);
        stepTitle.setId("wizard-step-title");

        stepContent = new VerticalLayout();
        stepContent.setSpacing(true);
        stepContent.setSizeFull();

        nextButton = new Button(getTranslation(I18n.Storefront.NEXT));
        nextButton.addStyleName(ValoTheme.BUTTON_PRIMARY);
        nextButton.addClickListener(e -> handleNext());

        prevButton = new Button(getTranslation(I18n.Storefront.PREVIOUS));
        prevButton.addClickListener(e -> handlePrevious());
        prevButton.setEnabled(false);

        var buttonLayout = new HorizontalLayout(prevButton, nextButton);
        buttonLayout.setSpacing(true);
        buttonLayout.setWidth("100%");

        root.addComponents(stepTitle, stepContent, buttonLayout);
        root.setExpandRatio(stepContent, 1.0f);

        setCompositionRoot(root);
        showStep(1);
    }

    private void showStep(int step) {
        currentStep = step;
        stepContent.removeAllComponents();
        quantityFields.clear();

        switch (step) {
        case 1:
            showStep1();
            break;
        case 2:
            showStep2();
            break;
        case 3:
            showStep3();
            break;
        case 4:
            showStep4();
            break;
        default:
            logger.error("Invalid step: {}", step);
        }

        prevButton.setEnabled(step > 1);
        nextButton.setCaption(step == 4 ? getTranslation(I18n.Storefront.SUBMIT)
                : getTranslation(I18n.Storefront.NEXT));
    }

    private void showStep1() {
        stepTitle.setValue(getTranslation(I18n.Storefront.STEP1_TITLE));

        productGrid = new Grid<>(ProductDto.class);
        productGrid.setSizeFull();
        productGrid.setSelectionMode(SelectionMode.MULTI);

        // Configure columns
        productGrid.removeAllColumns();
        productGrid.addColumn(ProductDto::getProductName)
                .setCaption(getTranslation(I18n.PRODUCT_NAME));
        productGrid.addColumn(ProductDto::getStockCount).setCaption("Stock");
        productGrid.addColumn(ProductDto::getPrice)
                .setCaption(getTranslation(I18n.PRICE))
                .setConverter(new EuroConverter(
                        getTranslation(I18n.Form.CANNOT_CONVERT)));

        // Add quantity column with NumberField
        productGrid.addComponentColumn(dto -> {
            var numberField = new NumberField(
                    getTranslation(I18n.Storefront.QUANTITY));
            numberField.setValue(dto.getOrderQuantity());
            numberField.setWidth("100px");
            numberField.addValueChangeListener(e -> {
                dto.setOrderQuantity(e.getValue());
                updateFooter();
            });
            quantityFields.put(dto.getProductId(), numberField);
            return numberField;
        }).setCaption(getTranslation(I18n.Storefront.QUANTITY));

        // Load products via presenter
        var products = presenter.getOrderableProducts();
        productGrid.setItems(products);

        // Add footer row for totals
        var footerRow = productGrid.appendFooterRow();
        footerRow.getCell(productGrid.getColumns().get(0))
                .setText(getTranslation(I18n.Storefront.ORDER_SUMMARY));
        footerRow.getCell(productGrid.getColumns().get(2)).setId("total-price");
        footerRow.getCell(productGrid.getColumns().get(3))
                .setId("total-quantity");

        updateFooter();

        stepContent.addComponent(productGrid);
        stepContent.setExpandRatio(productGrid, 1.0f);
    }

    private void updateFooter() {
        if (productGrid == null) {
            return;
        }

        var totalQuantity = 0;
        var totalPrice = BigDecimal.ZERO;

        for (var dto : productGrid.getDataProvider()
                .fetch(new com.vaadin.data.provider.Query<>())
                .collect(Collectors.toList())) {
            if (dto.getOrderQuantity() != null && dto.getOrderQuantity() > 0) {
                totalQuantity += dto.getOrderQuantity();
                totalPrice = totalPrice.add(dto.getLineTotal());
            }
        }

        var footerRow = productGrid.getFooterRow(0);
        var euroConverter = new EuroConverter("");
        footerRow.getCell(productGrid.getColumns().get(2))
                .setText(euroConverter.convertToPresentation(totalPrice, null));
        footerRow.getCell(productGrid.getColumns().get(3))
                .setText(String.valueOf(totalQuantity));
    }

    private void showStep2() {
        stepTitle.setValue(getTranslation(I18n.Storefront.STEP2_TITLE));

        addressBinder = new Binder<>(Address.class);

        streetField = new TextField(getTranslation(I18n.Storefront.STREET));
        streetField.setWidth("100%");
        addressBinder.forField(streetField)
                .asRequired(getTranslation(I18n.Storefront.STREET_REQUIRED))
                .bind(Address::getStreet, Address::setStreet);

        postalCodeField = new TextField(
                getTranslation(I18n.Storefront.POSTAL_CODE));
        addressBinder.forField(postalCodeField)
                .asRequired(
                        getTranslation(I18n.Storefront.POSTAL_CODE_REQUIRED))
                .bind(Address::getPostalCode, Address::setPostalCode);

        cityField = new TextField(getTranslation(I18n.Storefront.CITY));
        addressBinder.forField(cityField)
                .asRequired(getTranslation(I18n.Storefront.CITY_REQUIRED))
                .bind(Address::getCity, Address::setCity);

        countryField = new TextField(getTranslation(I18n.Storefront.COUNTRY));
        addressBinder.forField(countryField)
                .asRequired(getTranslation(I18n.Storefront.COUNTRY_REQUIRED))
                .bind(Address::getCountry, Address::setCountry);

        addressBinder.setBean(address);

        stepContent.addComponents(streetField, postalCodeField, cityField,
                countryField);
    }

    private void showStep3() {
        stepTitle.setValue(getTranslation(I18n.Storefront.STEP3_TITLE));

        supervisorComboBox = new ComboBox<>(
                getTranslation(I18n.Storefront.SUPERVISOR));
        supervisorComboBox.setWidth("100%");

        // Load supervisors via presenter
        var supervisors = presenter.getSupervisors();
        supervisorComboBox.setItems(supervisors);
        supervisorComboBox.setItemCaptionGenerator(User::getName);

        // Pre-select if already set
        if (selectedSupervisor != null) {
            supervisorComboBox.setValue(selectedSupervisor);
        }

        stepContent.addComponent(supervisorComboBox);
    }

    private void showStep4() {
        stepTitle.setValue(getTranslation(I18n.Storefront.STEP4_TITLE));

        var reviewHtml = buildReviewHtml();
        reviewLabel = new Label(reviewHtml, ContentMode.HTML);

        stepContent.addComponent(reviewLabel);
    }

    private String buildReviewHtml() {
        var sb = new StringBuilder();
        sb.append("<h3>").append(getTranslation(I18n.Storefront.ORDER_SUMMARY))
                .append("</h3>");

        sb.append("<h4>").append(getTranslation(I18n.Storefront.ITEMS_ORDERED))
                .append("</h4>");
        sb.append("<ul>");
        for (var entry : cart.getItems().entrySet()) {
            var product = entry.getKey();
            var quantity = entry.getValue();
            var lineTotal = product.getPrice()
                    .multiply(new java.math.BigDecimal(quantity));
            sb.append("<li>").append(product.getProductName()).append(" x ")
                    .append(quantity).append(" @ ").append(product.getPrice())
                    .append(" = ").append(lineTotal).append("</li>");
        }
        sb.append("</ul>");

        sb.append("<h4>")
                .append(getTranslation(I18n.Storefront.DELIVERY_ADDRESS))
                .append("</h4>");
        sb.append("<p>").append(address.toString()).append("</p>");

        sb.append("<h4>").append(getTranslation(I18n.Storefront.SUPERVISOR))
                .append("</h4>");
        sb.append("<p>")
                .append(selectedSupervisor != null
                        ? selectedSupervisor.getName()
                        : "N/A")
                .append("</p>");

        return sb.toString();
    }

    private void handleNext() {
        if (currentStep == 1) {
            // Build cart from selected products with quantities
            cart.clear();
            if (productGrid == null) {
                logger.error("productGrid is null in step 1");
                return;
            }

            var selectedProducts = productGrid.getSelectedItems();
            if (selectedProducts.isEmpty()) {
                Notification.show(getTranslation(I18n.Storefront.CART_EMPTY),
                        Notification.Type.WARNING_MESSAGE);
                return;
            }

            // Add products to cart - fetch actual Product entities from backend
            for (var dto : selectedProducts) {
                if (dto.getOrderQuantity() != null
                        && dto.getOrderQuantity() > 0) {
                    // Fetch the actual Product entity from backend by ID
                    var product = presenter.getProductById(dto.getProductId());
                    if (product != null) {
                        cart.setQuantity(product, dto.getOrderQuantity());
                    } else {
                        logger.error("Product with ID {} not found",
                                dto.getProductId());
                    }
                }
            }

            if (cart.isEmpty()) {
                Notification.show(getTranslation(I18n.Storefront.CART_EMPTY),
                        Notification.Type.WARNING_MESSAGE);
                return;
            }

            showStep(2);
        } else if (currentStep == 2) {
            if (addressBinder == null) {
                logger.error("addressBinder is null in step 2");
                return;
            }
            try {
                addressBinder.writeBean(address);
                showStep(3);
            } catch (ValidationException ex) {
                Notification.show(
                        getTranslation(I18n.Storefront.FILL_REQUIRED_FIELDS),
                        Notification.Type.WARNING_MESSAGE);
            }
        } else if (currentStep == 3) {
            if (supervisorComboBox == null) {
                logger.error("supervisorComboBox is null in step 3");
                return;
            }
            selectedSupervisor = supervisorComboBox.getValue();
            if (selectedSupervisor == null) {
                Notification.show(
                        getTranslation(I18n.Storefront.SELECT_SUPERVISOR),
                        Notification.Type.WARNING_MESSAGE);
                return;
            }
            showStep(4);
        } else if (currentStep == 4) {
            submitPurchase();
        }
    }

    private void handlePrevious() {
        if (currentStep > 1) {
            showStep(currentStep - 1);
        }
    }

    private void submitPurchase() {
        try {
            var currentUser = CurrentUser.get().orElseThrow(
                    () -> new IllegalStateException("User must be logged in"));

            var purchase = presenter.createPendingPurchase(cart, address,
                    currentUser, selectedSupervisor);

            logger.info("Purchase created: {}", purchase.getId());

            Notification.show(
                    getTranslation(I18n.Storefront.PURCHASE_CREATED,
                            purchase.getId()),
                    Notification.Type.TRAY_NOTIFICATION);

            // Reset wizard
            cart.clear();
            selectedSupervisor = null;
            showStep(1);

        } catch (Exception ex) {
            logger.error("Failed to create purchase", ex);
            Notification.show(getTranslation(I18n.Storefront.PURCHASE_FAILED),
                    Notification.Type.ERROR_MESSAGE);
        }
    }
}
