package org.vaadin.tatu.vaadincreate.admin;

import java.lang.reflect.Method;
import java.util.Collection;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.vaadin.tatu.vaadincreate.backend.data.Category;
import org.vaadin.tatu.vaadincreate.components.ConfirmDialog;
import org.vaadin.tatu.vaadincreate.components.AttributeExtension.HasAttributes;
import org.vaadin.tatu.vaadincreate.i18n.HasI18N;
import org.vaadin.tatu.vaadincreate.i18n.I18n;

import com.vaadin.data.BeanValidationBinder;
import com.vaadin.data.ValidationResult;
import com.vaadin.data.Validator;
import com.vaadin.data.ValueContext;
import com.vaadin.event.ConnectorEventListener;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.shared.Registration;
import com.vaadin.shared.ui.ValueChangeMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.Composite;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.TextField;
import com.vaadin.ui.themes.ValoTheme;
import com.vaadin.util.ReflectTools;

/**
 * The form for editing categories.
 */
@NullMarked
@SuppressWarnings({ "serial", "java:S2160" })
public class CategoryForm extends Composite implements HasI18N {

    private Category category;
    private TextField nameField;
    private BeanValidationBinder<Category> binder;
    private Button deleteButton;
    @Nullable
    private Collection<Category> categories;

    /**
     * Validator that checks that the category name is not a duplicate of an
     * existing category.
     */
    private class CategoryNotDuplicateValidator implements Validator<String> {
        @Override
        public ValidationResult apply(String value, ValueContext context) {
            if (categories == null) {
                // If categories haven't been set yet, validation passes
                return ValidationResult.ok();
            }

            boolean valid = categories.stream()
                    .filter(item -> !item.equals(category)
                            && item.getName().equals(value))
                    .count() == 0;

            return valid ? ValidationResult.ok()
                    : ValidationResult
                            .error(getTranslation(I18n.Category.DUPLICATE));
        }
    }

    public CategoryForm(Category category) {
        this.category = category;
        nameField = new NameField(category);
        nameField.addFocusListener(
                focus -> fireEvent(new FormFocusEvent(this, true)));
        nameField.addBlurListener(
                blur -> fireEvent(new FormFocusEvent(this, false)));
        // Focus the name field if the category is new
        if (category.getId() == null) {
            nameField.focus();
        }

        deleteButton = new Button(VaadinIcons.TRASH,
                click -> handleConfirmDelete());
        deleteButton.addStyleName(ValoTheme.BUTTON_DANGER);
        deleteButton.setDescription(String.format("%s: %s",
                getTranslation(I18n.DELETE), category.getName()));
        deleteButton.setEnabled(category.getId() != null);
        // Button is discarded upon refresh and replaced with a new one,
        // so no need to re-enable it
        deleteButton.setDisableOnClick(true);
        deleteButton.setWidth("45px");

        binder = new BeanValidationBinder<>(Category.class);
        setupBinder();

        var layout = new HorizontalLayout(nameField, deleteButton);
        layout.setExpandRatio(nameField, 1);
        layout.setWidthFull();
        setCompositionRoot(layout);
    }

    /**
     * Sets up the binder with validators
     */
    private void setupBinder() {
        binder.forField(nameField)
                .withValidator(new CategoryNotDuplicateValidator())
                .bind("name");
        binder.setBean(category);
        binder.addValueChangeListener(valueChange -> {
            if (binder.isValid()) {
                fireEvent(new FormSaveEvent(this, category));
            }
        });
    }

    /**
     * Sets the categories used for validation.
     * 
     * @param categories
     *            collection of categories to validate against
     * @return this CategoryForm for method chaining
     */
    public CategoryForm withCategories(Collection<Category> categories) {
        this.categories = categories;
        return this;
    }

    private void handleConfirmDelete() {
        var dialog = new ConfirmDialog(getTranslation(I18n.CONFIRM),
                getTranslation(I18n.WILL_DELETE, category.getName()),
                ConfirmDialog.Type.ALERT);
        dialog.setConfirmText(getTranslation(I18n.DELETE));
        dialog.setCancelText(getTranslation(I18n.CANCEL));
        dialog.open();
        dialog.addConfirmedListener(
                confirmed -> fireEvent(new FormDeleteEvent(this, category)));
        dialog.addCancelledListener(cancelled -> deleteButton.setEnabled(true));
    }

    /**
     * Sets focus to the name field in this form.
     */
    @Override
    public void focus() {
        if (nameField != null) {
            nameField.focus();
        }
    }

    /**
     * Add event listener for FormSaveEvent. Event is fired when category is
     * modified through the form.
     *
     * @param listener
     *            The listener, can be Lambda expression.
     * @return Registration Use Registration#remove() for listener removal.
     */
    public Registration addFormSaveListener(FormSaveListener listener) {
        return addListener(FormSaveEvent.class, listener,
                FormSaveListener.FORM_SAVE_METHOD);
    }

    /**
     * Add event listener for FormDeleteEvent. Event is fired when delete button
     * is clicked and confirmed.
     *
     * @param listener
     *            The listener, can be Lambda expression.
     * @return Registration Use Registration#remove() for listener removal.
     */
    public Registration addFormDeleteListener(FormDeleteListener listener) {
        return addListener(FormDeleteEvent.class, listener,
                FormDeleteListener.FORM_DELETE_METHOD);
    }

    /**
     * Add event listener for FormFocusEvent. Event is fired when the form field
     * gains or loses focus.
     *
     * @param listener
     *            The listener, can be Lambda expression.
     * @return Registration Use Registration#remove() for listener removal.
     */
    public Registration addFormFocusListener(FormFocusListener listener) {
        return addListener(FormFocusEvent.class, listener,
                FormFocusListener.FORM_FOCUS_METHOD);
    }

    /**
     * FormSaveEvent listener interface, can be implemented with Lambda or
     * anonymous inner class.
     */
    public interface FormSaveListener extends ConnectorEventListener {
        Method FORM_SAVE_METHOD = ReflectTools.findMethod(
                FormSaveListener.class, "formSave", FormSaveEvent.class);

        public void formSave(FormSaveEvent event);
    }

    /**
     * FormDeleteEvent listener interface, can be implemented with Lambda or
     * anonymous inner class.
     */
    public interface FormDeleteListener extends ConnectorEventListener {
        Method FORM_DELETE_METHOD = ReflectTools.findMethod(
                FormDeleteListener.class, "formDelete", FormDeleteEvent.class);

        public void formDelete(FormDeleteEvent event);
    }

    /**
     * FormFocusEvent listener interface, can be implemented with Lambda or
     * anonymous inner class.
     */
    public interface FormFocusListener extends ConnectorEventListener {
        Method FORM_FOCUS_METHOD = ReflectTools.findMethod(
                FormFocusListener.class, "formFocus", FormFocusEvent.class);

        public void formFocus(FormFocusEvent event);
    }

    /**
     * FormSaveEvent is fired when a category is edited.
     */
    public static class FormSaveEvent extends Component.Event {
        private Category category;

        public FormSaveEvent(Component source, Category category) {
            super(source);
            this.category = category;
        }

        /**
         * Returns the category being saved.
         *
         * @return the category
         */
        public Category getCategory() {
            return category;
        }
    }

    /**
     * FormDeleteEvent is fired when delete button is clicked and confirmed.
     */
    public static class FormDeleteEvent extends Component.Event {
        private Category category;

        public FormDeleteEvent(Component source, Category category) {
            super(source);
            this.category = category;
        }

        /**
         * Returns the category being deleted.
         *
         * @return the category
         */
        public Category getCategory() {
            return category;
        }
    }

    /**
     * FormFocusEvent is fired when the form field gains or loses focus.
     */
    public static class FormFocusEvent extends Component.Event {
        private boolean focused;

        public FormFocusEvent(Component source, boolean focused) {
            super(source);
            this.focused = focused;
        }

        /**
         * Returns whether the form field is focused.
         *
         * @return true if focused, false if blurred
         */
        public boolean isFocused() {
            return focused;
        }
    }

    static class NameField extends TextField
            implements HasI18N, HasAttributes<NameField> {

        public NameField(Category category) {
            super();
            setAttribute("autocomplete", "off");
            setAriaLabel(getTranslation(I18n.Category.CATEGORY_NAME));
            removeAttribute("aria-labelledby");
            setId(String.format("name-%s", category.getId()));
            setValueChangeMode(ValueChangeMode.LAZY);
            setValueChangeTimeout(2000);
            setWidthFull();
            setPlaceholder(getTranslation(I18n.Category.INSTRUCTION));
        }
    }
}