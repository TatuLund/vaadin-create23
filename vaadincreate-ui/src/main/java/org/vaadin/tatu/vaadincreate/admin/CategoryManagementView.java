package org.vaadin.tatu.vaadincreate.admin;

import java.util.ArrayList;
import java.util.Collection;

import org.vaadin.tatu.vaadincreate.backend.data.Category;
import org.vaadin.tatu.vaadincreate.i18n.HasI18N;

import com.vaadin.data.BeanValidationBinder;
import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.shared.ui.ValueChangeMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Grid.SelectionMode;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

@SuppressWarnings("serial")
public class CategoryManagementView extends VerticalLayout
        implements TabView, HasI18N {

    public static final String VIEW_NAME = "categories";

    private static final String DELETE = "delete";
    private static final String CATEGORY_DELETED = "category-deleted";
    private static final String CATEGORY_SAVED = "category-saved";
    private static final String ADD_NEW_CATEGORY = "add-new-category";
    private static final String ADMIN = "admin";
    private static final String EDIT_CATEGORIES = "edit-categories";

    private CategoryManagmentPresenter presenter = new CategoryManagmentPresenter(
            this);
    private Grid<Category> categoriesListing;
    private ListDataProvider<Category> dataProvider;
    private Button newCategoryButton;

    public CategoryManagementView() {
        setSizeFull();
        categoriesListing = new Grid<>();
        categoriesListing.setRowHeight(40);
        categoriesListing.addComponentColumn(this::createCategoryEditor);
        categoriesListing.setHeaderRowHeight(1);
        categoriesListing.setSizeFull();
        categoriesListing.setSelectionMode(SelectionMode.NONE);

        newCategoryButton = new Button(getTranslation(ADD_NEW_CATEGORY),
                event -> {
                    Category category = new Category();
                    dataProvider.getItems().add(category);
                    dataProvider.refreshAll();
                    categoriesListing.scrollToEnd();
                });
        newCategoryButton.setDisableOnClick(true);
        newCategoryButton.setId("new-category");

        var h2 = new Label(getTranslation(ADMIN));
        h2.addStyleName(ValoTheme.LABEL_H2);
        var h4 = new Label(getTranslation(EDIT_CATEGORIES));
        h4.addStyleName(ValoTheme.LABEL_H4);

        addComponents(h2, h4, newCategoryButton, categoriesListing);
        setExpandRatio(categoriesListing, 1);
    }

    @Override
    public void attach() {
        super.attach();
        presenter.requestUpdateCategories();
    }

    public void setCategories(Collection<Category> categories) {
        dataProvider = new ListDataProvider<Category>(
                new ArrayList<>(categories)) {
            @Override
            public Object getId(Category item) {
                return item.getId();
            }
        };
        categoriesListing.setDataProvider(dataProvider);
    }

    private Component createCategoryEditor(Category category) {
        var nameField = new TextField();
        nameField.setValueChangeMode(ValueChangeMode.LAZY);
        nameField.setValueChangeTimeout(1000);
        nameField.setWidthFull();
        if (category.getId() < 0) {
            nameField.focus();
        }

        var deleteButton = new Button(VaadinIcons.MINUS_CIRCLE_O, event -> {
            presenter.removeCategory(category);
            dataProvider.getItems().remove(category);
            dataProvider.refreshAll();
            Notification.show(getTranslation(CATEGORY_DELETED, category.getName()));
        });
        deleteButton.addStyleName(ValoTheme.BUTTON_DANGER);
        deleteButton.setDescription(getTranslation(DELETE));

        var<Category> binder = new BeanValidationBinder<Category>(
                Category.class);
        binder.forField(nameField).bind("name");
        binder.setBean(category);
        binder.addValueChangeListener(event -> {
            if (binder.isValid()) {
                var newCategory = presenter.updateCategory(category);
                if (category.getId() == -1) {
                    dataProvider.getItems().remove(category);
                    dataProvider.getItems().add(newCategory);
                    dataProvider.refreshAll();
                    nameField.focus();
                } else {
                    dataProvider.getItems().remove(category);
                    dataProvider.getItems().add(newCategory);
                    dataProvider.refreshItem(newCategory);
                }
                deleteButton.setEnabled(true);
                newCategoryButton.setEnabled(true);
                Notification.show(getTranslation(CATEGORY_SAVED, newCategory.getName()));
            }
        });
        deleteButton.setEnabled(category.getId() > 0);

        var layout = new HorizontalLayout(nameField, deleteButton);
        layout.setExpandRatio(nameField, 1);
        layout.setWidthFull();
        return layout;
    }

    @Override
    public String getTabName() {
        return VIEW_NAME;
    }

}
