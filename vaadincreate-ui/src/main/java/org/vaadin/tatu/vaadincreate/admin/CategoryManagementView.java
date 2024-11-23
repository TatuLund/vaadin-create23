package org.vaadin.tatu.vaadincreate.admin;

import java.util.ArrayList;
import java.util.Collection;

import org.vaadin.tatu.vaadincreate.AttributeExtension;
import org.vaadin.tatu.vaadincreate.ConfirmDialog;
import org.vaadin.tatu.vaadincreate.backend.data.Category;
import org.vaadin.tatu.vaadincreate.i18n.HasI18N;
import org.vaadin.tatu.vaadincreate.i18n.I18n;

import com.vaadin.data.BeanValidationBinder;
import com.vaadin.data.ValidationResult;
import com.vaadin.data.Validator;
import com.vaadin.data.ValueContext;
import com.vaadin.event.ShortcutListener;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.shared.ui.ValueChangeMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.Composite;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

@SuppressWarnings({ "serial", "java:S2160" })
public class CategoryManagementView extends VerticalLayout
        implements TabView, HasI18N {

    public static final String VIEW_NAME = "categories";

    private CategoryManagementPresenter presenter = new CategoryManagementPresenter(
            this);
    private Button newCategoryButton;

    private Collection<Category> categories;

    private ComponentList<Category, CategoryForm> list;

    class EscapeListener extends ShortcutListener {
        EscapeListener() {
            super("Cancel", KeyCode.ESCAPE, new int[0]);
        }

        @Override
        public void handleAction(Object sender, Object target) {
            presenter.requestUpdateCategories();
            newCategoryButton.setEnabled(true);
        }
    }

    @SuppressWarnings("java:S5669")
    public CategoryManagementView() {
        setSizeFull();
        list = new ComponentList<>(CategoryForm::new);

        newCategoryButton = new Button(
                getTranslation(I18n.Category.ADD_NEW_CATEGORY),
                event -> addCategory());
        newCategoryButton.setIcon(VaadinIcons.PLUS_CIRCLE);
        newCategoryButton.addStyleName(ValoTheme.BUTTON_FRIENDLY);
        newCategoryButton.setDisableOnClick(true);
        newCategoryButton.setId("new-category");

        var h4 = new Label(getTranslation(I18n.Category.EDIT_CATEGORIES));
        h4.addStyleName(ValoTheme.LABEL_H4);

        // Cancel the form when the user presses escape
        addShortcutListener(new EscapeListener());

        addComponents(h4, newCategoryButton, list);
        setExpandRatio(list, 1);
    }

    private void addCategory() {
        var newCategory = new Category();
        list.addItem(newCategory);
    }

    @Override
    public void enter() {
        presenter.requestUpdateCategories();
        newCategoryButton.setEnabled(true);
    }

    /**
     * Sets the categories for the CategoryManagementView.
     *
     * @param categories
     *            the collection of categories to be set
     */
    public void setCategories(Collection<Category> categories) {
        this.categories = categories;
        list.setItems(new ArrayList<>(categories));
    }

    @Override
    public String getTabName() {
        return VIEW_NAME;
    }

    public void showSaveConflict() {
        Notification.show(getTranslation(I18n.SAVE_CONFLICT),
                Notification.Type.WARNING_MESSAGE);
        newCategoryButton.setEnabled(true);
    }

    public void showDeleteError() {
        Notification.show(getTranslation(I18n.Category.DELETE_ERROR),
                Notification.Type.WARNING_MESSAGE);
    }

    public void showDeleted(String name) {
        Notification.show(getTranslation(I18n.Category.CATEGORY_DELETED, name));
    }

    /**
     * Validator to ensure that a category is not duplicated.
     */
    class CategoryNotDuplicateValidator implements Validator<String> {

        private Category category;
        private String errorMessage;

        /**
         * Validator to ensure that a category is not duplicated.
         *
         * @param category
         *            the category to be validated
         * @param errorMessage
         *            the error message to be displayed if the category is
         *            duplicated
         */
        public CategoryNotDuplicateValidator(Category category,
                String errorMessage) {
            this.category = category;
            this.errorMessage = errorMessage;
        }

        @Override
        public ValidationResult apply(String value, ValueContext context) {
            var valid = categories.stream()
                    .filter(item -> !item.equals(category)
                            && item.getName().equals(value))
                    .count() == 0;
            if (valid) {
                return ValidationResult.ok();
            }
            return ValidationResult.error(errorMessage);
        }
    }

    /**
     * A form for editing a category.
     */
    class CategoryForm extends Composite {

        private Category category;
        private TextField nameField;
        private BeanValidationBinder<Category> binder;
        private Button deleteButton;

        CategoryForm(Category category) {
            this.category = category;
            configureNameField();
            // Focus the name field if the category is new
            if (category.getId() == null) {
                nameField.focus();
            }

            deleteButton = new Button(VaadinIcons.TRASH,
                    e -> handleConfirmDelete());
            deleteButton.addStyleName(ValoTheme.BUTTON_DANGER);
            deleteButton.setDescription(String.format("%s: %s",
                    getTranslation(I18n.DELETE), category.getName()));
            deleteButton.setEnabled(category.getId() != null);
            deleteButton.setDisableOnClick(true);

            binder = new BeanValidationBinder<>(Category.class);
            // Check for duplicate category names
            binder.forField(nameField)
                    .withValidator(new CategoryNotDuplicateValidator(category,
                            getTranslation(I18n.Category.DUPLICATE)))
                    .bind("name");
            binder.setBean(category);
            binder.addValueChangeListener(event -> handleSave());

            var layout = new HorizontalLayout(nameField, deleteButton);
            layout.setExpandRatio(nameField, 1);
            layout.setWidthFull();
            setCompositionRoot(layout);
        }

        private void configureNameField() {
            nameField = new TextField();
            var nameFieldExt = AttributeExtension.of(nameField);
            nameFieldExt.setAttribute("autocomplete", "off");
            nameFieldExt.setAttribute("aria-label",
                    getTranslation(I18n.Category.CATEGORY));
            nameFieldExt.removeAttribute("aria-labelledby");
            nameField.setId(String.format("name-%s", category.getId()));
            nameField.setValueChangeMode(ValueChangeMode.LAZY);
            nameField.setValueChangeTimeout(2000);
            nameField.setWidthFull();
            nameField.setPlaceholder(getTranslation(I18n.Category.INSTRUCTION));
            nameField
                    .addFocusListener(e -> newCategoryButton.setEnabled(false));
            nameField.addBlurListener(e -> newCategoryButton.setEnabled(true));
        }

        private void handleSave() {
            if (binder.isValid()) {
                var saved = presenter.updateCategory(category);
                if (saved != null) {
                    list.replaceItem(category, saved);
                    if (category.getId() == null) {
                        nameField.focus();
                    }
                    presenter.requestUpdateCategories();
                    deleteButton.setEnabled(true);
                    newCategoryButton.setEnabled(true);
                    Notification.show(getTranslation(
                            I18n.Category.CATEGORY_SAVED, saved.getName()));
                }
            }
        }

        // Handle the delete button click event with a confirmation dialog.
        private void handleConfirmDelete() {
            var dialog = new ConfirmDialog(
                    getTranslation(I18n.WILL_DELETE, category.getName()),
                    ConfirmDialog.Type.ALERT);
            dialog.setConfirmText(getTranslation(I18n.DELETE));
            dialog.setCancelText(getTranslation(I18n.CANCEL));
            dialog.open();
            dialog.addConfirmedListener(e -> {
                presenter.removeCategory(category);
                list.removeItem(category);
                categories.remove(category);
            });
            dialog.addCancelListener(e -> deleteButton.setEnabled(true));
        }
    }
}
