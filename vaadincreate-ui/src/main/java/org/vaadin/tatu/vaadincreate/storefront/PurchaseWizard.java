package org.vaadin.tatu.vaadincreate.storefront;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Collection;
import java.util.Map.Entry;
import java.util.Set;

import org.jsoup.Jsoup;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.tatu.vaadincreate.VaadinCreateTheme;
import org.vaadin.tatu.vaadincreate.auth.CurrentUser;
import org.vaadin.tatu.vaadincreate.backend.data.Address;
import org.vaadin.tatu.vaadincreate.backend.data.Cart;
import org.vaadin.tatu.vaadincreate.backend.data.Product;
import org.vaadin.tatu.vaadincreate.backend.data.User;
import org.vaadin.tatu.vaadincreate.common.EuroConverter;
import org.vaadin.tatu.vaadincreate.common.EuroRenderer;
import org.vaadin.tatu.vaadincreate.common.NumberField;
import org.vaadin.tatu.vaadincreate.i18n.HasI18N;
import org.vaadin.tatu.vaadincreate.components.AttributeExtension;
import org.vaadin.tatu.vaadincreate.components.AttributeExtension.HasAttributes;
import org.vaadin.tatu.vaadincreate.components.Html;
import org.vaadin.tatu.vaadincreate.components.AttributeExtension.AriaAttributes;
import org.vaadin.tatu.vaadincreate.components.AttributeExtension.AriaRoles;
import org.vaadin.tatu.vaadincreate.i18n.I18n;
import org.vaadin.tatu.vaadincreate.observability.Telemetry;
import org.vaadin.tatu.vaadincreate.util.Utils;

import com.vaadin.data.Binder;
import com.vaadin.data.ValidationException;
import com.vaadin.data.provider.Query;
import com.vaadin.event.ShortcutAction.ModifierKey;
import com.vaadin.shared.data.sort.SortDirection;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Composite;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Grid;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.JavaScript;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.components.grid.FooterRow;
import com.vaadin.ui.components.grid.MultiSelectionModel;
import com.vaadin.ui.components.grid.MultiSelectionModel.SelectAllCheckBoxVisibility;
import com.vaadin.ui.themes.ValoTheme;

import elemental.events.KeyboardEvent.KeyCode;

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
    private ProductGrid productGrid;

    // Step 2 components
    @Nullable
    private AddressForm addressForm;

    // Step 3 components
    @Nullable
    private ComboBox<User> supervisorComboBox;

    // Step 4 components
    @Nullable
    private Label reviewLabel;

    private StorefrontPresenter presenter;

    /**
     * Creates the purchase wizard with the given presenter for backend
     * interactions.
     *
     * @param presenter
     *            the presenter for backend interactions
     */
    public PurchaseWizard(StorefrontPresenter presenter) {
        this.presenter = presenter;
        root = new VerticalLayout();
        root.setMargin(true);
        root.setSpacing(true);
        root.setSizeFull();
        root.addStyleNames(VaadinCreateTheme.STOREFRONTVIEW_WIZARD,
                ValoTheme.LAYOUT_WELL);

        stepTitle = new Label();
        stepTitle.addStyleName(ValoTheme.LABEL_H2);
        stepTitle.setId("wizard-step-title");
        AttributeExtension.of(stepTitle).setAttribute(AriaAttributes.LIVE,
                "polite");

        stepContent = new VerticalLayout();
        stepContent.addStyleName(ValoTheme.LAYOUT_CARD);
        stepContent.setSpacing(true);
        stepContent.setMargin(false);
        stepContent.setSizeFull();
        AttributeExtension.of(stepContent).setAttribute(AriaAttributes.ROLE,
                AriaRoles.REGION);
        AttributeExtension.of(stepContent)
                .setAttribute(AriaAttributes.LABELLEDBY, stepTitle.getId());

        nextButton = new Button(getTranslation(I18n.Storefront.NEXT));
        nextButton.addStyleName(ValoTheme.BUTTON_PRIMARY);
        nextButton.addClickListener(e -> handleNext());
        nextButton.setClickShortcut(KeyCode.N, ModifierKey.ALT);
        AttributeExtension.of(nextButton)
                .setAttribute(AriaAttributes.KEYSHORTCUTS, "Alt+N");

        prevButton = new Button(getTranslation(I18n.Storefront.PREVIOUS));
        prevButton.addClickListener(e -> handlePrevious());
        prevButton.setEnabled(false);
        prevButton.setClickShortcut(KeyCode.P, ModifierKey.ALT);
        AttributeExtension.of(prevButton)
                .setAttribute(AriaAttributes.KEYSHORTCUTS, "Alt+P");

        var buttonLayout = new HorizontalLayout(prevButton, nextButton);
        buttonLayout.setSpacing(true);
        buttonLayout.setWidth("100%");
        buttonLayout.setComponentAlignment(prevButton, Alignment.MIDDLE_LEFT);
        buttonLayout.setComponentAlignment(nextButton, Alignment.MIDDLE_RIGHT);

        root.addComponents(stepTitle, stepContent, buttonLayout);
        root.setExpandRatio(stepContent, 1.0f);

        setCompositionRoot(root);
        showStep(1);
    }

    private void showStep(int step) {
        currentStep = step;
        stepContent.removeAllComponents();

        switch (step) {
        case 1 -> showStep1();
        case 2 -> showStep2();
        case 3 -> showStep3();
        case 4 -> showStep4();
        default -> logger.error("Invalid step: {}", step);
        }

        prevButton.setEnabled(step > 1);
        nextButton.setCaption(step == 4 ? getTranslation(I18n.Storefront.SUBMIT)
                : getTranslation(I18n.Storefront.NEXT));
    }

    private void showStep1() {
        stepTitle.setValue(getTranslation(I18n.Storefront.STEP1_TITLE));

        if (productGrid != null) {
            stepContent.setMargin(false);
            stepContent.addComponent(productGrid);
            stepContent.setExpandRatio(productGrid, 1.0f);
            return;
        }

        productGrid = new ProductGrid();

        productGrid.updateFooter();

        stepContent.addComponent(productGrid);
        stepContent.setExpandRatio(productGrid, 1.0f);
    }

    /**
     * Sets the products to be displayed in the product selection grid. If the
     * collection is empty, shows a message indicating that no products are
     * available.
     */
    public void setProducts(Collection<ProductDto> products) {
        if (products.isEmpty()) {
            stepContent.addComponent(new Label(
                    getTranslation(I18n.Storefront.NO_PRODUCTS_AVAILABLE)));
            return;
        }
        productGrid.setItems(products);
        productGrid.updateFooter();
    }

    private void showStep2() {
        stepTitle.setValue(getTranslation(I18n.Storefront.STEP2_TITLE));
        applyDefaultAddressIfAvailable();

        addressForm = new AddressForm(address);
        stepContent.addComponent(addressForm);
        stepContent.setMargin(true);
    }

    private void showStep3() {
        stepTitle.setValue(getTranslation(I18n.Storefront.STEP3_TITLE));
        applyDefaultSupervisorIfAvailable();

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
        stepContent.setMargin(true);
        supervisorComboBox.focus();
    }

    private void applyDefaultAddressIfAvailable() {
        if (!isAddressEmpty()) {
            return;
        }
        presenter.getDefaultAddress(getCurrentUser())
                .ifPresent(this::copyAddressToWizardState);
    }

    private void applyDefaultSupervisorIfAvailable() {
        if (selectedSupervisor != null) {
            return;
        }
        selectedSupervisor = presenter.getDefaultSupervisor(getCurrentUser())
                .orElse(null);
    }

    private User getCurrentUser() {
        return CurrentUser.get().orElseThrow(
                () -> new IllegalStateException("User must be logged in"));
    }

    private boolean isAddressEmpty() {
        return address.getStreet().isBlank()
                && address.getPostalCode().isBlank()
                && address.getCity().isBlank()
                && address.getCountry().isBlank();
    }

    private void copyAddressToWizardState(Address defaultAddress) {
        address.setStreet(defaultAddress.getStreet());
        address.setPostalCode(defaultAddress.getPostalCode());
        address.setCity(defaultAddress.getCity());
        address.setCountry(defaultAddress.getCountry());
    }

    private void showStep4() {
        stepTitle.setValue(getTranslation(I18n.Storefront.STEP4_TITLE));

        var reviewHtml = buildReviewHtml();
        reviewLabel = new Label(reviewHtml, ContentMode.HTML);

        stepContent.addComponent(reviewLabel);
        stepContent.setMargin(true);

        Notification.show(Jsoup.parse(reviewHtml).text(),
                Notification.Type.ASSISTIVE_NOTIFICATION);

        nextButton.focus();
    }

    private String buildReviewHtml() {
        var euroFormat = EuroConverter.createEuroFormat();

        var div = Html.div();

        div.add(Html.h3().text(getTranslation(I18n.Storefront.ORDER_SUMMARY)));

        div.add(Html.h4().text(getTranslation(I18n.Storefront.ITEMS_ORDERED)));

        var list = Html.ul();
        for (var entry : cart.getItems().entrySet()) {
            var itemText = getItemText(euroFormat, entry);
            list.add(Html.li().text(itemText));
        }
        div.add(list);

        div.add(Html.h4()
                .text(getTranslation(I18n.Storefront.DELIVERY_ADDRESS)));
        div.add(Html.p().text(address.toString()));

        div.add(Html.h4().text(getTranslation(I18n.Storefront.SUPERVISOR)));
        var supervisorName = selectedSupervisor != null
                ? selectedSupervisor.getName()
                : "N/A";
        div.add(Html.p().text(supervisorName));

        return div.build();
    }

    private String getItemText(NumberFormat euroFormat,
            Entry<Product, Integer> entry) {
        var product = entry.getKey();
        var quantity = entry.getValue();
        var lineTotal = product.getPrice()
                .multiply(new BigDecimal(quantity));

        return String.format("%s x %d @ %s = %s",
                product.getProductName(), quantity,
                euroFormat.format(product.getPrice()),
                euroFormat.format(lineTotal));
    }

    private void handleNext() {
        switch (currentStep) {
        case 1 -> {
            // Build cart from selected products with quantities
            cart.clear();
            assert productGrid != null : "productGrid is null in step 1";

            var selectedProducts = productGrid.getSelectedItems();
            if (selectedProducts.isEmpty()) {
                Notification.show(getTranslation(I18n.Storefront.CART_EMPTY),
                        Notification.Type.WARNING_MESSAGE);
                return;
            }

            addSelectedProductsToCart(selectedProducts);

            if (cart.isEmpty()) {
                Notification.show(getTranslation(I18n.Storefront.CART_EMPTY),
                        Notification.Type.WARNING_MESSAGE);
                return;
            }

            showStep(2);
        }
        case 2 -> {
            assert addressForm != null : "addressForm is null in step 2";
            saveAddressDataAndShowStep3(addressForm.getBinder());
        }
        case 3 -> {
            assert supervisorComboBox != null
                    : "supervisorComboBox is null in step 3";
            selectedSupervisor = supervisorComboBox.getValue();
            if (selectedSupervisor == null) {
                Notification.show(
                        getTranslation(I18n.Storefront.SELECT_SUPERVISOR),
                        Notification.Type.WARNING_MESSAGE);
                return;
            }
            showStep(4);
        }
        case 4 -> submitPurchase();
        default -> {
            // No-op for unexpected step values.
        }
        }
    }

    private void saveAddressDataAndShowStep3(Binder<Address> binder) {
        try {
            binder.writeBean(address);
            showStep(3);
        } catch (ValidationException ex) {
            Notification.show(
                    getTranslation(I18n.Storefront.FILL_REQUIRED_FIELDS),
                    Notification.Type.WARNING_MESSAGE);
        }
    }

    private void addSelectedProductsToCart(Set<ProductDto> selectedProducts) {
        // Add products to cart - fetch actual Product entities from backend
        for (var dto : selectedProducts) {
            addProductToCart(dto);
        }
    }

    private void addProductToCart(ProductDto dto) {
        if (dto.getOrderQuantity() > 0) {
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
            Telemetry.saveItem(purchase);

            logger.info("Purchase created: {}", purchase.getId());

            Notification.show(
                    getTranslation(I18n.Storefront.PURCHASE_CREATED,
                            purchase.getId()),
                    Notification.Type.TRAY_NOTIFICATION);

            // Reset wizard
            cart.clear();
            productGrid.setItems(presenter.getOrderableProducts());
            selectedSupervisor = null;
            showStep(1);

        } catch (Exception ex) {
            logger.error("Failed to create purchase", ex);
            Notification.show(getTranslation(I18n.Storefront.PURCHASE_FAILED),
                    Notification.Type.ERROR_MESSAGE);
        }
    }

    @SuppressWarnings("java:S110")
    static class AddressForm extends FormLayout
            implements HasAttributes<AddressForm>, HasI18N {

        private final Binder<Address> binder;

        /**
         * Creates an address form bound to the given Address bean. The form
         * includes fields for street, postal code, city, and country, all of
         * which are required.
         *
         * @param address
         *            the Address bean to bind to the form
         */
        public AddressForm(Address address) {
            binder = new Binder<>(Address.class);
            setRole("form");

            var streetField = new ATextField(
                    getTranslation(I18n.Storefront.STREET));
            streetField.setWidth("100%");
            streetField.setAutocomplete("street-address");
            binder.forField(streetField)
                    .asRequired(getTranslation(
                            I18n.Storefront.STREET_REQUIRED))
                    .bind(Address::getStreet, Address::setStreet);

            var postalCodeField = new ATextField(
                    getTranslation(I18n.Storefront.POSTAL_CODE));
            postalCodeField.setAutocomplete("postal-code");
            binder.forField(postalCodeField)
                    .asRequired(getTranslation(
                            I18n.Storefront.POSTAL_CODE_REQUIRED))
                    .bind(Address::getPostalCode, Address::setPostalCode);

            var cityField = new ATextField(
                    getTranslation(I18n.Storefront.CITY));
            cityField.setAutocomplete("address-level2");
            binder.forField(cityField)
                    .asRequired(getTranslation(I18n.Storefront.CITY_REQUIRED))
                    .bind(Address::getCity, Address::setCity);

            var countryField = new ATextField(
                    getTranslation(I18n.Storefront.COUNTRY));
            countryField.setAutocomplete("country");
            binder.forField(countryField)
                    .asRequired(getTranslation(
                            I18n.Storefront.COUNTRY_REQUIRED))
                    .bind(Address::getCountry, Address::setCountry);

            binder.setBean(address);
            addComponents(streetField, postalCodeField, cityField,
                    countryField);
            streetField.focus();
        }

        /**
         * Returns the Binder used for this address form, allowing the caller to
         * write the form data back to the Address bean and check for
         * validation.
         */
        public Binder<Address> getBinder() {
            return binder;
        }
    }

    @SuppressWarnings("java:S110")
    static class ATextField extends TextField
            implements HasAttributes<ATextField> {

        public ATextField(String caption) {
            super(caption);
        }

        /**
         * Sets the autocomplete attribute for this text field to improve user
         * experience by enabling browser autofill features. The value should be
         * a valid autocomplete token as defined in HTML specifications, such as
         * "street-address", "postal-code", "country", etc.
         */
        public void setAutocomplete(String value) {
            setAttribute("autocomplete", value);
        }
    }

    static class ProductGrid extends Grid<ProductDto> implements HasI18N {

        private final FooterRow footerRow;

        private ProductGrid() {
            super(ProductDto.class);
            setId("purchase-grid");
            setSizeFull();
            setSelectionMode(SelectionMode.MULTI);
            setAccessibleNavigation(true);

            // Configure columns
            removeAllColumns();
            addColumn(ProductDto::getProductName)
                    .setCaption(getTranslation(I18n.PRODUCT_NAME))
                    .setMaximumWidth(300)
                    .setComparator((p1, p2) -> p1.getProductName()
                            .compareToIgnoreCase(p2.getProductName()));
            addColumn(ProductDto::getStockCount)
                    .setCaption(getTranslation(I18n.IN_STOCK))
                    .setComparator((p1, p2) -> Integer
                            .compare(p1.getStockCount(), p2.getStockCount()));
            addColumn(ProductDto::getPrice, new EuroRenderer())
                    .setCaption(getTranslation(I18n.PRICE)).setComparator(
                            (p1, p2) -> p1.getPrice().compareTo(p2.getPrice()));

            // Add quantity column with NumberField
            addComponentColumn(dto -> {
                var layout = new HorizontalLayout();
                layout.setMargin(false);
                layout.setSpacing(false);
                layout.setWidth("100%");
                if (getSelectedItems().contains(dto)) {
                    var numberField = new NumberField(null);
                    numberField.setAriaLabel(
                            getTranslation(I18n.Storefront.QUANTITY));
                    numberField.setValue(dto.getOrderQuantity());
                    numberField.setWidth("100%");
                    numberField.addValueChangeListener(e -> {
                        dto.setOrderQuantity(e.getValue());
                        updateFooter();
                        sort("quantity-column", SortDirection.DESCENDING);
                    });
                    numberField.setId("quantity-for-" + dto.getProductId());
                    layout.addComponent(numberField);
                } else {
                    layout.addComponent(new Label("-"));
                }
                return layout;
            }).setComparator((p1, p2) -> Integer.compare(p1.getOrderQuantity(),
                    p2.getOrderQuantity()))
                    .setCaption(getTranslation(I18n.Storefront.QUANTITY))
                    .setId("quantity-column").setStyleGenerator(
                            product -> VaadinCreateTheme.STOREFRONTVIEW_WIZARD_QUANTITYCOLUMN);

            // Footer row for totals
            footerRow = appendFooterRow();
            footerRow.getCell(getColumns().get(0))
                    .setText(getTranslation(I18n.Storefront.ORDER_SUMMARY));

            ((MultiSelectionModel<ProductDto>) this.getSelectionModel())
                    .setSelectAllCheckBoxVisibility(
                            SelectAllCheckBoxVisibility.HIDDEN);

            // Update quantities when selection changes
            addSelectionListener(e -> {
                for (var dto : getDataProvider().fetch(new Query<>())
                        .toList()) {
                    if (!e.getAllSelectedItems().contains(dto)) {
                        dto.setOrderQuantity(0);
                    }
                }
                getDataProvider().refreshAll();
                updateFooter();
            });
        }

        private void updateFooter() {
            var totalQuantity = 0;
            var totalPrice = BigDecimal.ZERO;

            for (var dto : getDataProvider().fetch(new Query<>()).toList()) {
                if (dto.getOrderQuantity() > 0) {
                    totalQuantity += dto.getOrderQuantity();
                    totalPrice = totalPrice.add(dto.getLineTotal());
                }
            }

            var euroConverter = new EuroConverter("");
            footerRow.getCell(getColumns().get(2))
                    .setText(euroConverter.convertToPresentation(totalPrice,
                            Utils.createValueContext()));
            footerRow.getCell(getColumns().get(3))
                    .setText(String.valueOf(totalQuantity));
        }

        @Override
        public void attach() {
            super.attach();
            patchQuantityColumnFocus();
        }

        private void patchQuantityColumnFocus() {
            // Grid re-renders (e.g., refreshAll()) regenerate cell DOM. This
            // patch is
            // therefore both (1) installed once and (2) re-applicable to newly
            // generated cells/inputs without stacking listeners.
            JavaScript.getCurrent().execute(
                    """
                            (function() {
                                window.__vaadincreateQuantityPatch = window.__vaadincreateQuantityPatch || {};
                                var state = window.__vaadincreateQuantityPatch;

                                function isQuantityCellElement(el) {
                                    return el && el.closest && el.closest('td.storefrontview-wizard-quantitycolumn');
                                }

                                function applyPatch() {
                                    // Patch any newly created inputs in quantity cells.
                                    document.querySelectorAll('td.storefrontview-wizard-quantitycolumn input').forEach(function(input) {
                                        if (input.__vaadincreateQuantityPatched) {
                                            return;
                                        }
                                        input.__vaadincreateQuantityPatched = true;
                                        input.addEventListener('keydown', function(e) {
                                            if (e.key === 'ArrowUp' || e.key === 'ArrowDown') {
                                                e.preventDefault();
                                            }
                                        }, true);
                                    });
                                }

                                // Expose so Java can re-apply after refreshAll().
                                state.apply = applyPatch;

                                if (!state.installed) {
                                    state.installed = true;

                                    // When a quantity cell receives focus, forward focus to its input.
                                    document.addEventListener('focusin', function(e) {
                                        var cell = isQuantityCellElement(e.target);
                                        if (!cell) {
                                            return;
                                        }
                                        var input = cell.querySelector('input');
                                        if (input && e.target !== input) {
                                            input.focus();
                                        }
                                    }, true);

                                    // Observe DOM changes so newly generated cells also get patched.
                                    // (Still safe to call state.apply() from Java after refreshAll().)
                                    state.observer = new MutationObserver(function() {
                                        applyPatch();
                                    });
                                    if (document.body) {
                                        state.observer.observe(document.body, { childList: true, subtree: true });
                                    }
                                }

                                // Apply immediately for current DOM.
                                state.apply();
                            })();
                            """);
        }
    }
}
