package org.vaadin.tatu.vaadincreate.admin;

import java.util.ArrayList;
import java.util.Collection;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.vaadin.tatu.vaadincreate.AttributeExtension.AriaAttributes;
import org.vaadin.tatu.vaadincreate.AttributeExtension.AriaRoles;
import org.vaadin.tatu.vaadincreate.AttributeExtension.HasAttributes;
import org.vaadin.tatu.vaadincreate.backend.data.Category;
import org.vaadin.tatu.vaadincreate.i18n.I18n;

import com.vaadin.event.ShortcutListener;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.shared.Registration;
import com.vaadin.ui.Button;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

@NullMarked
@SuppressWarnings({ "serial", "java:S2160" })
public class CategoryManagementView extends VerticalLayout
        implements TabView, HasAttributes<CategoryManagementView> {

    public static final String VIEW_NAME = "categories";

    private CategoryManagementPresenter presenter = new CategoryManagementPresenter(
            this);
    private Button newCategoryButton;

    @Nullable
    private Collection<Category> categories;

    private ComponentList<Category, CategoryForm> categoryList;

    private Registration shortcutRegistration;

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
        setRole(AriaRoles.REGION);
        setAttribute(AriaAttributes.LABELLEDBY, "view-name");

        categoryList = new ComponentList<>(this::createCategoryForm);

        newCategoryButton = new Button(
                getTranslation(I18n.Category.ADD_NEW_CATEGORY),
                click -> addCategory());
        newCategoryButton.setIcon(VaadinIcons.PLUS_CIRCLE);
        newCategoryButton.addStyleName(ValoTheme.BUTTON_FRIENDLY);
        newCategoryButton.setDisableOnClick(true);
        newCategoryButton.setId("new-category");

        var viewName = new Label(getTranslation(I18n.Category.EDIT_CATEGORIES));
        viewName.addStyleName(ValoTheme.LABEL_H4);
        viewName.setId("view-name");

        // Cancel the form when the user presses escape
        shortcutRegistration = addShortcutListener(new EscapeListener());

        addComponents(viewName, newCategoryButton, categoryList);
        setExpandRatio(categoryList, 1);
    }

    private CategoryForm createCategoryForm(Category category) {
        CategoryForm form = new CategoryForm(category)
                .withCategories(categories);
        form.addFormSaveListener(event -> handleSave(event.getCategory()));
        form.addFormDeleteListener(event -> handleDelete(event.getCategory()));
        form.addFormFocusListener(
                event -> newCategoryButton.setEnabled(!event.isFocused()));
        return form;
    }

    private void handleSave(Category category) {
        var saved = presenter.updateCategory(category);
        if (saved != null) {
            categoryList.replaceItem(category, saved);

            if (category.getId() == null) {
                // If this was a new category, focus the form for easier data
                // entry
                var form = categoryList.getComponentFor(saved);
                if (form != null) {
                    form.focus();
                }
            }

            presenter.requestUpdateCategories();
            newCategoryButton.setEnabled(true);
            Notification.show(getTranslation(I18n.Category.CATEGORY_SAVED,
                    saved.getName()));
        }
    }

    private void handleDelete(Category category) {
        presenter.removeCategory(category);
        categoryList.removeItem(category);
        if (categories != null) {
            categories.remove(category);
        }
    }

    private void addCategory() {
        var newCategory = new Category();
        categoryList.addItem(newCategory);
    }

    @Override
    public void detach() {
        super.detach();
        shortcutRegistration.remove();
    }

    @Override
    public void enter() {
        openingView(VIEW_NAME);
        presenter.requestUpdateCategories();
        newCategoryButton.setEnabled(true);
        newCategoryButton.focus();
    }

    /**
     * Sets the categories for the CategoryManagementView.
     *
     * @param categories
     *            the collection of categories to be set
     */
    public void setCategories(Collection<Category> categories) {
        this.categories = categories;
        categoryList.setItems(new ArrayList<>(categories));
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
}
