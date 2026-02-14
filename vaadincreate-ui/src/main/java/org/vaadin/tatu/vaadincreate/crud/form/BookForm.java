package org.vaadin.tatu.vaadincreate.crud.form;

import java.math.BigDecimal;
import java.util.Collection;
import java.lang.reflect.Method;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.tatu.vaadincreate.VaadinCreateTheme;
import org.vaadin.tatu.vaadincreate.VaadinCreateUI;
import org.vaadin.tatu.vaadincreate.auth.AccessControl;
import org.vaadin.tatu.vaadincreate.backend.data.Availability;
import org.vaadin.tatu.vaadincreate.backend.data.Category;
import org.vaadin.tatu.vaadincreate.backend.data.Product;
import org.vaadin.tatu.vaadincreate.common.EuroConverter;
import org.vaadin.tatu.vaadincreate.common.NumberField;
import org.vaadin.tatu.vaadincreate.components.AttributeExtension;
import org.vaadin.tatu.vaadincreate.components.CharacterCountExtension;
import org.vaadin.tatu.vaadincreate.components.ConfirmDialog;
import org.vaadin.tatu.vaadincreate.components.Html;
import org.vaadin.tatu.vaadincreate.components.AttributeExtension.AriaAttributes;
import org.vaadin.tatu.vaadincreate.components.AttributeExtension.AriaRoles;
import org.vaadin.tatu.vaadincreate.components.AttributeExtension.HasAttributes;
import org.vaadin.tatu.vaadincreate.components.ConfirmDialog.Type;
import org.vaadin.tatu.vaadincreate.i18n.HasI18N;
import org.vaadin.tatu.vaadincreate.i18n.I18n;
import org.vaadin.tatu.vaadincreate.observability.Telemetry;
import org.vaadin.tatu.vaadincreate.util.Utils;

import com.vaadin.data.BeanValidationBinder;
import com.vaadin.data.Binder;
import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.event.ShortcutAction.ModifierKey;
import com.vaadin.event.ShortcutListener;
import com.vaadin.event.ConnectorEventListener;
import com.vaadin.server.AbstractErrorMessage;
import com.vaadin.server.Page;
import com.vaadin.server.UserError;
import com.vaadin.shared.Registration;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.shared.ui.ErrorLevel;
import com.vaadin.ui.AbstractComponent;
import com.vaadin.ui.Button;
import com.vaadin.ui.CheckBoxGroup;
import com.vaadin.ui.Composite;
import com.vaadin.ui.Component;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Notification;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;
import com.vaadin.util.ReflectTools;

/**
 * Form component for creating or editing a Product (book) in Vaadin Create.
 *
 * Key features:
 * <ul>
 * <li>Fields: productName, price, stockCount, availability, category
 * (multi-select).</li>
 * <li>Binding: BeanValidationBinder with field and bean-level (availability vs
 * stock) validation.</li>
 * <li>Event driven: emits save, delete, discard, cancel, navigate
 * (next/previous), and draft save events instead of invoking a presenter
 * directly (decoupled form).</li>
 * <li>Dirty tracking: changed fields highlighted; tooltip shows previous value
 * (formatted).</li>
 * <li>Draft support: unsaved changes auto-saved as a draft on detach; draft can
 * be merged (mergeDraft).</li>
 * <li>Accessibility: ARIA roles/attributes, assistive notifications, keyboard
 * shortcuts (Ctrl+S, Escape, PageDown, PageUp).</li>
 * <li>Category handling: setCategories(Collection) populates and sorts with
 * currently selected first.</li>
 * <li>Deletion confirmation dialog; guarded by access control.</li>
 * </ul>
 * Typical usage:
 * <ol>
 * <li>showForm(true); editProduct(product);</li>
 * <li>Register listeners (addSaveListener, addDeleteListener, etc.).</li>
 * <li>Before closing, call hasChanges() for confirmation logic.</li>
 * </ol>
 * UI thread only.
 */
@NullMarked
@SuppressWarnings({ "serial", "java:S2160" })
public class BookForm extends Composite implements HasI18N {

    private static final Logger logger = LoggerFactory
            .getLogger(BookForm.class);

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

    private AccessControl accessControl = VaadinCreateUI.get()
            .getAccessControl();
    private final Binder<Product> binder;

    @Nullable
    private Product currentProduct;
    private SidePanel sidePanel = new SidePanel();
    private boolean visible;
    private boolean isValid;
    private Registration pageDownRegistration;
    private Registration pageUpRegistration;

    /**
     * Creates a new decoupled BookForm. External logic (presenter, grid
     * selection, navigation etc.) must be handled by listeners registered in
     * the owning view.
     */
    @SuppressWarnings("java:S5669")
    public BookForm() {
        setCompositionRoot(sidePanel);
        buildForm();

        binder = new BeanValidationBinder<>(Product.class);
        binder.forField(price)
                .withConverter(new EuroConverter(
                        getTranslation(I18n.Form.CANNOT_CONVERT)))
                .bind("price");
        binder.forField(stockCount).bind("stockCount");

        category.setItemCaptionGenerator(Category::getName);
        binder.forField(category).bind("category");

        // Add bean level validation for Availability vs. Stock count cross
        // checking. Note, "Error" is not shown.
        binder.withValidator(this::checkAvailabilityVsStockCount, "Error");

        binder.bindInstanceFields(this);
        binder.setChangeDetectionEnabled(true);

        // enable/disable save button while editing
        binder.addStatusChangeListener(statusChange -> {
            isValid = !statusChange.hasValidationErrors();
            if (isValid) {
                setStockCountAndAvailabilityInvalid(false);
            }
            if (!isValid) {
                saveButton.setEnabled(false);
            }
        });

        binder.addValueChangeListener(valueChange -> {
            var hasChanges = binder.hasChanges();
            saveButton.setEnabled(hasChanges && isValid);
            discardButton.setEnabled(hasChanges);
        });

        saveButton.addClickListener(clicked -> handleSave());
        saveButton.setClickShortcut(KeyCode.S, ModifierKey.CTRL);
        AttributeExtension.of(saveButton)
                .setAttribute(AriaAttributes.KEYSHORTCUTS, "Control+S");

        discardButton.addClickListener(clicked -> handleDiscard());

        cancelButton
                .addClickListener(clicked -> fireEvent(new CancelEvent(this)));
        cancelButton.setClickShortcut(KeyCode.ESCAPE);
        AttributeExtension.of(cancelButton)
                .setAttribute(AriaAttributes.KEYSHORTCUTS, "Escape");

        deleteButton.addClickListener(clicked -> handleDelete());
        pageDownRegistration = addShortcutListener(
                new ShortcutListener("Next", KeyCode.PAGE_DOWN, null) {
                    @Override
                    public void handleAction(Object sender, Object target) {
                        fireEvent(new NavigateNextEvent(BookForm.this,
                                currentProduct));
                    }
                });
        pageUpRegistration = addShortcutListener(
                new ShortcutListener("Previous", KeyCode.PAGE_UP, null) {
                    @Override
                    public void handleAction(Object sender, Object target) {
                        fireEvent(new NavigatePreviousEvent(BookForm.this,
                                currentProduct));
                    }
                });
    }

    private void handleSave() {
        if (currentProduct != null && binder.writeBeanIfValid(currentProduct)) {
            fireEvent(new SaveEvent(this, currentProduct));
            return;
        }
        setStockCountAndAvailabilityInvalid(binderHasInvalidFieldsBound());
    }

    private void handleDiscard() {
        if (currentProduct != null) {
            binder.readBean(currentProduct);
            saveButton.setEnabled(false);
            discardButton.setEnabled(false);
            updateDirtyIndicators();
        }
        fireEvent(new DiscardEvent(this, currentProduct));
    }

    private void handleDelete() {
        if (currentProduct != null) {
            var dialog = new ConfirmDialog(getTranslation(I18n.CONFIRM),
                    getTranslation(I18n.WILL_DELETE,
                            currentProduct.getProductName()),
                    Type.ALERT);
            dialog.setConfirmText(getTranslation(I18n.DELETE));
            dialog.setCancelText(getTranslation(I18n.CANCEL));
            dialog.open();
            dialog.addConfirmedListener(
                    e -> fireEvent(new DeleteEvent(this, currentProduct)));
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
            Notification.show(getTranslation(I18n.Form.AVAILABILITY_MISMATCH),
                    Notification.Type.ASSISTIVE_NOTIFICATION);
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
        if (!visible) {
            Notification.show(getTranslation(I18n.CLOSED),
                    Notification.Type.ASSISTIVE_NOTIFICATION);
        }
    }

    @Nullable
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
            assert currentProduct != null;
            var value = binding.getGetter().apply(currentProduct);
            if (value != null) {
                var html = Html.div()
                        .add(Html.b().text(getTranslation(I18n.Form.WAS)))
                        .add(Html.br()).raw(convertValue(value)).build();
                field.setDescription(html, ContentMode.HTML);
            }
        });
    }

    @SuppressWarnings("java:S2259")
    private static <T> String convertValue(@Nullable T value) {
        return switch (value) {
        // bug in SonarQube, does not recognize null case here S2259
        case null -> "";
        case BigDecimal price -> {
            var euroConverter = new EuroConverter("");
            yield Utils.convertToPresentation(price, euroConverter);
        }
        case Availability availability -> {
            var icon = Utils.createAvailabilityIcon(availability).build();
            var span = Html.span().style("margin-right: 5px")
                    .text(availability.toString()).build();
            yield icon + span;
        }
        default -> value.toString();
        };
    }

    private void clearDirtyIndicators() {
        binder.getFields().forEach(hasValue -> {
            var field = (AbstractComponent) hasValue;
            field.removeStyleName(VaadinCreateTheme.BOOKFORM_FIELD_DIRTY);
            field.setDescription(null);
        });
    }

    private void buildForm() {
        var formLayout = new Form();
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

        AttributeExtension.of(category).setAttribute(AriaAttributes.ROLE,
                AriaRoles.GROUP);
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

        var spacer = new CssLayout();

        formLayout.addComponents(productName, fieldWrapper, availability,
                category);
        formLayout.addComponents(spacer);
        formLayout.addComponents(saveButton, discardButton, cancelButton,
                deleteButton);
        formLayout.setExpandRatio(spacer, 1);

        sidePanel.setContent(formLayout);
        sidePanel.setAriaLabel(getTranslation(I18n.Books.PRODUCT_FORM));
    }

    @SuppressWarnings("unchecked")
    public void setCategories(Collection<Category> categories) {
        category.setItems(categories);
        // Show selected items first in the list
        var dataProvider = (ListDataProvider<Category>) category
                .getDataProvider();
        dataProvider.setSortComparator(
                (a, b) -> category.getValue().contains(a) ? -1 : 1);
        var product = getProduct();
        if (product != null) {
            category.setValue(product.getCategory());
        }
    }

    public void editProduct(@Nullable Product product) {
        accessControl.assertAdmin();
        if (product == null) {
            product = new Product();
            readProduct(product);
            return;
        }
        readProduct(product);

        // Scroll to the top
        // As this is not a Panel, using JavaScript
        if (isAttached()) {
            var scrollScript = String.format(
                    "window.document.getElementById('%s').scrollTop = 0;",
                    getId());
            Page.getCurrent().getJavaScript().execute(scrollScript);
        }

        announceProductOpened(product);
        Telemetry.openedItem(product);
    }

    private void readProduct(Product product) {
        deleteButton.setEnabled(product.getId() != null);
        currentProduct = product;
        binder.readBean(product);
        saveButton.setEnabled(false);
        discardButton.setEnabled(false);
    }

    // This is horrible, but required to workaround a bug in NVDA
    private void announceProductOpened(Product product) {
        if (isAttached()) {
            Notification.show(
                    String.format("%s %s", product.getProductName(),
                            getTranslation(I18n.OPENED)),
                    Notification.Type.ASSISTIVE_NOTIFICATION);
        }
    }

    @Override
    public void detach() {
        super.detach();
        cancelButton.removeClickShortcut();
        saveButton.removeClickShortcut();
        pageDownRegistration.remove();
        pageUpRegistration.remove();
        if (isShown() && binder.hasChanges()) {
            logger.info(
                    "Browser closed before saving changes, draft product autosaved.");
            var product = getProduct();
            assert product != null;
            var draft = new Product(product);
            binder.writeBeanAsDraft(draft, true);
            fireEvent(new DraftSaveEvent(this, draft));
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

    /**
     * Reset current form field values back to the backing bean and clear dirty
     * indicators so that the form is considered clean. Used after a failed save
     * caused by concurrent category deletion so that Cancel can close the form
     * without a confirmation dialog.
     */
    public void resetChanges() {
        if (currentProduct != null) {
            binder.readBean(currentProduct);
        }
        saveButton.setEnabled(false);
        discardButton.setEnabled(false);
        clearDirtyIndicators();
    }

    /**
     * Add a listener that is notified when the user clicks the Save button.
     * 
     * @param saveListener
     *            the listener to add
     * @return a registration for the listener
     */
    public Registration addSaveListener(SaveListener saveListener) {
        return addListener(SaveEvent.class, saveListener, SaveListener.METHOD);
    }

    /**
     * Add a listener that is notified when the user clicks the Delete button.
     * 
     * @param deleteListener
     *            the listener to add
     * @return a registration for the listener
     */
    public Registration addDeleteListener(DeleteListener deleteListener) {
        return addListener(DeleteEvent.class, deleteListener,
                DeleteListener.METHOD);
    }

    /**
     * Add a listener that is notified when the user clicks the Discard button.
     * 
     * @param discardListener
     *            the listener to add
     * @return a registration for the listener
     */
    public Registration addDiscardListener(DiscardListener discardListener) {
        return addListener(DiscardEvent.class, discardListener,
                DiscardListener.METHOD);
    }

    /**
     * Add a listener that is notified when the user clicks the Cancel button.
     * 
     * @param cancelListener
     *            the listener to add
     * @return a registration for the listener
     */
    public Registration addCancelListener(CancelListener cancelListener) {
        return addListener(CancelEvent.class, cancelListener,
                CancelListener.METHOD);
    }

    /**
     * Add a listener that is notified when the user navigates to the next
     * 
     * @param navigateNextListener
     *            the listener to add
     * @return a registration for the listener
     */
    public Registration addNavigateNextListener(
            NavigateNextListener navigateNextListener) {
        return addListener(NavigateNextEvent.class, navigateNextListener,
                NavigateNextListener.METHOD);
    }

    /**
     * Add a listener that is notified when the user navigates to the previous
     * 
     * @param navigatePreviousListener
     *            the listener to add
     * @return a registration for the listener
     */
    public Registration addNavigatePreviousListener(
            NavigatePreviousListener navigatePreviousListener) {
        return addListener(NavigatePreviousEvent.class,
                navigatePreviousListener, NavigatePreviousListener.METHOD);
    }

    /**
     * Add a listener that is notified when the user navigates to the previous
     * 
     * @param draftSaveListener
     *            the listener to add
     * @return a registration for the listener
     */
    public Registration addDraftSaveListener(
            DraftSaveListener draftSaveListener) {
        return addListener(DraftSaveEvent.class, draftSaveListener,
                DraftSaveListener.METHOD);
    }

    /**
     * A form component that extends VerticalLayout and sets ARIA attributes to
     * enhance accessibility. The form is given a tabindex, an aria-label for
     * screen readers, a role of "form", and aria-keyshortcuts for keyboard
     * navigation.
     */
    public class Form extends VerticalLayout implements HasAttributes<Form> {

        /**
         * Constructs a new Form instance and sets ARIA attributes to enhance
         * accessibility.
         */
        public Form() {
            super();
            // Set ARIA attributes for the form to make it accessible
            setAttribute("tabindex", 0);
            setRole(AriaRoles.FORM);
            setAttribute(AriaAttributes.KEYSHORTCUTS, "Escape PageDown PageUp");
        }
    }

    abstract static class AbstractBookFormEvent extends Component.Event {
        private static final long serialVersionUID = 1L;
        @Nullable
        private final Product product;

        protected AbstractBookFormEvent(Component source,
                @Nullable Product product) {
            super(source);
            this.product = product;
        }

        @Nullable
        public Product getProduct() {
            return product;
        }
    }

    public static class SaveEvent extends AbstractBookFormEvent {
        private static final long serialVersionUID = 1L;

        public SaveEvent(Component source, Product product) {
            super(source, product);
        }
    }

    public static class DeleteEvent extends AbstractBookFormEvent {
        private static final long serialVersionUID = 1L;

        public DeleteEvent(Component source, Product product) {
            super(source, product);
        }
    }

    public static class DiscardEvent extends AbstractBookFormEvent {
        private static final long serialVersionUID = 1L;

        public DiscardEvent(Component source, @Nullable Product product) {
            super(source, product);
        }
    }

    public static class CancelEvent extends AbstractBookFormEvent {
        private static final long serialVersionUID = 1L;

        public CancelEvent(Component source) {
            super(source, null);
        }
    }

    public static class NavigateNextEvent extends AbstractBookFormEvent {
        private static final long serialVersionUID = 1L;

        public NavigateNextEvent(Component source, @Nullable Product product) {
            super(source, product);
        }
    }

    public static class NavigatePreviousEvent extends AbstractBookFormEvent {
        private static final long serialVersionUID = 1L;

        public NavigatePreviousEvent(Component source,
                @Nullable Product product) {
            super(source, product);
        }
    }

    public static class DraftSaveEvent extends AbstractBookFormEvent {
        private static final long serialVersionUID = 1L;

        public DraftSaveEvent(Component source, Product product) {
            super(source, product);
        }
    }

    // Listener interfaces
    public interface SaveListener extends ConnectorEventListener {
        Method METHOD = ReflectTools.findMethod(SaveListener.class, "onSave",
                SaveEvent.class);

        void onSave(SaveEvent e);
    }

    public interface DeleteListener extends ConnectorEventListener {
        Method METHOD = ReflectTools.findMethod(DeleteListener.class,
                "onDelete", DeleteEvent.class);

        void onDelete(DeleteEvent e);
    }

    public interface DiscardListener extends ConnectorEventListener {
        Method METHOD = ReflectTools.findMethod(DiscardListener.class,
                "onDiscard", DiscardEvent.class);

        void onDiscard(DiscardEvent e);
    }

    public interface CancelListener extends ConnectorEventListener {
        Method METHOD = ReflectTools.findMethod(CancelListener.class,
                "onCancel", CancelEvent.class);

        void onCancel(CancelEvent e);
    }

    public interface NavigateNextListener extends ConnectorEventListener {
        Method METHOD = ReflectTools.findMethod(NavigateNextListener.class,
                "onNext", NavigateNextEvent.class);

        void onNext(NavigateNextEvent e);
    }

    public interface NavigatePreviousListener extends ConnectorEventListener {
        Method METHOD = ReflectTools.findMethod(NavigatePreviousListener.class,
                "onPrevious", NavigatePreviousEvent.class);

        void onPrevious(NavigatePreviousEvent e);
    }

    public interface DraftSaveListener extends ConnectorEventListener {
        Method METHOD = ReflectTools.findMethod(DraftSaveListener.class,
                "onDraftSave", DraftSaveEvent.class);

        void onDraftSave(DraftSaveEvent e);
    }

}
