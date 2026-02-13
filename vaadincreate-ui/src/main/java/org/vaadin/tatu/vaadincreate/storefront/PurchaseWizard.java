package org.vaadin.tatu.vaadincreate.storefront;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.tatu.vaadincreate.VaadinCreateTheme;
import org.vaadin.tatu.vaadincreate.VaadinCreateUI;
import org.vaadin.tatu.vaadincreate.auth.CurrentUser;
import org.vaadin.tatu.vaadincreate.backend.ProductDataService;
import org.vaadin.tatu.vaadincreate.backend.PurchaseService;
import org.vaadin.tatu.vaadincreate.backend.UserService;
import org.vaadin.tatu.vaadincreate.backend.data.Address;
import org.vaadin.tatu.vaadincreate.backend.data.Availability;
import org.vaadin.tatu.vaadincreate.backend.data.Cart;
import org.vaadin.tatu.vaadincreate.backend.data.Product;
import org.vaadin.tatu.vaadincreate.backend.data.Purchase;
import org.vaadin.tatu.vaadincreate.backend.data.User;
import org.vaadin.tatu.vaadincreate.backend.data.User.Role;
import org.vaadin.tatu.vaadincreate.i18n.HasI18N;
import org.vaadin.tatu.vaadincreate.i18n.I18n;

import com.vaadin.data.Binder;
import com.vaadin.data.ValidationException;
import com.vaadin.data.validator.IntegerRangeValidator;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Composite;
import com.vaadin.ui.Grid;
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

    // Step 1 components
    @Nullable
    private Grid<Product> productGrid;
    @Nullable
    private TextField quantityField;

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

    private final ProductDataService productService = VaadinCreateUI.get()
            .getProductService();
    private final UserService userService = VaadinCreateUI.get()
            .getUserService();
    private final PurchaseService purchaseService = PurchaseService.get();

    public PurchaseWizard() {
        root = new VerticalLayout();
        root.setMargin(true);
        root.setSpacing(true);
        root.setSizeFull();
        root.addStyleName(VaadinCreateTheme.STOREFRONTVIEW_WIZARD);

        stepTitle = new Label();
        stepTitle.addStyleName(ValoTheme.LABEL_H2);

        stepContent = new VerticalLayout();
        stepContent.setSpacing(true);

        nextButton = new Button(getTranslation(I18n.Storefront.NEXT));
        nextButton.addStyleName(ValoTheme.BUTTON_PRIMARY);
        nextButton.addClickListener(e -> handleNext());

        prevButton = new Button(getTranslation(I18n.Storefront.PREVIOUS));
        prevButton.addClickListener(e -> handlePrevious());
        prevButton.setEnabled(false);

        root.addComponents(stepTitle, stepContent, prevButton, nextButton);

        setCompositionRoot(root);
        showStep(1);
    }

    private void showStep(int step) {
        currentStep = step;
        stepContent.removeAllComponents();

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
        nextButton.setCaption(step == 4
                ? getTranslation(I18n.Storefront.SUBMIT)
                : getTranslation(I18n.Storefront.NEXT));
    }

    private void showStep1() {
        stepTitle.setValue(getTranslation(I18n.Storefront.STEP1_TITLE));

        productGrid = new Grid<>(Product.class);
        productGrid.setColumns("productName", "price", "stockCount");
        productGrid.setSizeFull();

        // Load orderable products (AVAILABLE and stock > 0)
        var products = productService.getAllProducts().stream()
                .filter(p -> p.getAvailability() == Availability.AVAILABLE
                        && p.getStockCount() > 0)
                .collect(Collectors.toList());
        productGrid.setItems(products);

        quantityField = new TextField(
                getTranslation(I18n.Storefront.QUANTITY));
        quantityField.setValue("1");

        var addToCartButton = new Button(
                getTranslation(I18n.Storefront.ADD_TO_CART));
        addToCartButton.addStyleName(ValoTheme.BUTTON_FRIENDLY);
        addToCartButton.addClickListener(e -> addToCart());

        var cartItemsLabel = new Label(
                getTranslation(I18n.Storefront.CART_ITEMS) + ": "
                        + cart.size());
        cartItemsLabel.setId("cart-items-label");

        stepContent.addComponents(productGrid, quantityField, addToCartButton,
                cartItemsLabel);
    }

    private void addToCart() {
        var selected = productGrid.asSingleSelect().getValue();
        if (selected == null) {
            Notification.show(
                    getTranslation(I18n.Storefront.SELECT_PRODUCT_FIRST),
                    Notification.Type.WARNING_MESSAGE);
            return;
        }

        try {
            var quantity = Integer.parseInt(quantityField.getValue());
            if (quantity <= 0) {
                Notification.show(
                        getTranslation(I18n.Storefront.QUANTITY_POSITIVE),
                        Notification.Type.WARNING_MESSAGE);
                return;
            }

            cart.setQuantity(selected, quantity);
            Notification.show(
                    getTranslation(I18n.Storefront.ADDED_TO_CART,
                            selected.getProductName(), quantity),
                    Notification.Type.TRAY_NOTIFICATION);

            // Update cart count
            var cartLabel = (Label) stepContent.getComponent(3);
            cartLabel.setValue(getTranslation(I18n.Storefront.CART_ITEMS)
                    + ": " + cart.size());
        } catch (NumberFormatException ex) {
            Notification.show(getTranslation(I18n.Storefront.INVALID_QUANTITY),
                    Notification.Type.ERROR_MESSAGE);
        }
    }

    private void showStep2() {
        stepTitle.setValue(getTranslation(I18n.Storefront.STEP2_TITLE));

        addressBinder = new Binder<>(Address.class);

        streetField = new TextField(getTranslation(I18n.Storefront.STREET));
        streetField.setWidth("100%");
        addressBinder.forField(streetField).asRequired(
                getTranslation(I18n.Storefront.STREET_REQUIRED))
                .bind(Address::getStreet, Address::setStreet);

        postalCodeField = new TextField(
                getTranslation(I18n.Storefront.POSTAL_CODE));
        addressBinder.forField(postalCodeField).asRequired(
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

        // Load users with USER or ADMIN roles
        var supervisors = userService.getAllUsers().stream()
                .filter(u -> u.getRole() == Role.USER
                        || u.getRole() == Role.ADMIN)
                .collect(Collectors.toList());
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

        sb.append("<h4>")
                .append(getTranslation(I18n.Storefront.ITEMS_ORDERED))
                .append("</h4>");
        sb.append("<ul>");
        for (var entry : cart.getItems().entrySet()) {
            var product = entry.getKey();
            var quantity = entry.getValue();
            var lineTotal = product.getPrice().multiply(
                    new java.math.BigDecimal(quantity));
            sb.append("<li>").append(product.getProductName()).append(" x ")
                    .append(quantity).append(" @ ").append(product.getPrice())
                    .append(" = ").append(lineTotal).append("</li>");
        }
        sb.append("</ul>");

        sb.append("<h4>").append(getTranslation(I18n.Storefront.DELIVERY_ADDRESS))
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
            if (cart.isEmpty()) {
                Notification.show(
                        getTranslation(I18n.Storefront.CART_EMPTY),
                        Notification.Type.WARNING_MESSAGE);
                return;
            }
            showStep(2);
        } else if (currentStep == 2) {
            try {
                addressBinder.writeBean(address);
                showStep(3);
            } catch (ValidationException ex) {
                Notification.show(
                        getTranslation(I18n.Storefront.FILL_REQUIRED_FIELDS),
                        Notification.Type.WARNING_MESSAGE);
            }
        } else if (currentStep == 3) {
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
            var currentUser = CurrentUser.get()
                    .orElseThrow(() -> new IllegalStateException(
                            "User must be logged in"));

            var purchase = purchaseService.createPendingPurchase(cart, address,
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
            Notification.show(
                    getTranslation(I18n.Storefront.PURCHASE_FAILED),
                    Notification.Type.ERROR_MESSAGE);
        }
    }
}
