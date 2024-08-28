package org.vaadin.tatu.vaadincreate.admin;

import java.util.ArrayList;
import java.util.Collection;

import javax.persistence.OptimisticLockException;

import org.vaadin.tatu.vaadincreate.ConfirmDialog;
import org.vaadin.tatu.vaadincreate.VaadinCreateTheme;
import org.vaadin.tatu.vaadincreate.backend.data.Category;
import org.vaadin.tatu.vaadincreate.i18n.HasI18N;
import org.vaadin.tatu.vaadincreate.i18n.I18n;

import com.vaadin.data.BeanValidationBinder;
import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.event.ShortcutListener;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.shared.ui.ValueChangeMode;
import com.vaadin.shared.ui.grid.HeightMode;
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

@SuppressWarnings({ "serial", "java:S2160" })
public class CategoryManagementView extends VerticalLayout
        implements TabView, HasI18N {

    public static final String VIEW_NAME = "categories";

    private CategoryManagementPresenter presenter = new CategoryManagementPresenter(
            this);
    private Grid<Category> categoriesListing;
    private ListDataProvider<Category> dataProvider;
    private Button newCategoryButton;

    private Collection<Category> categories;

    private Category newCategory;

    public CategoryManagementView() {
        setSizeFull();
        createCategoryListing();

        newCategoryButton = new Button(
                getTranslation(I18n.Category.ADD_NEW_CATEGORY), event -> {
                    newCategory = new Category();
                    dataProvider.getItems().add(newCategory);
                    dataProvider.refreshAll();
                    categoriesListing
                            .setHeightByRows(dataProvider.getItems().size());
                    categoriesListing.scrollToEnd();
                });
        newCategoryButton.setIcon(VaadinIcons.PLUS_CIRCLE);
        newCategoryButton.addStyleName(ValoTheme.BUTTON_FRIENDLY);
        newCategoryButton.setDisableOnClick(true);
        newCategoryButton.setId("new-category");

        var h4 = new Label(getTranslation(I18n.Category.EDIT_CATEGORIES));
        h4.addStyleName(ValoTheme.LABEL_H4);

        addComponents(h4, newCategoryButton, categoriesListing);
        setExpandRatio(categoriesListing, 1);
    }

    private void createCategoryListing() {
        categoriesListing = new Grid<>();
        categoriesListing.setRowHeight(40);
        categoriesListing.addComponentColumn(this::createCategoryEditor);
        categoriesListing.setHeaderRowHeight(1);
        categoriesListing.setSizeFull();
        categoriesListing.setSelectionMode(SelectionMode.NONE);
        categoriesListing.addStyleNames(
                VaadinCreateTheme.ADMINVIEW_CATEGORY_GRID,
                VaadinCreateTheme.GRID_NO_STRIPES,
                VaadinCreateTheme.GRID_NO_BORDERS,
                VaadinCreateTheme.GRID_NO_CELL_FOCUS);
        categoriesListing.setHeightMode(HeightMode.ROW);
    }

    @Override
    public void enter() {
        presenter.requestUpdateCategories();
    }

    /**
     * Sets the categories for the CategoryManagementView.
     *
     * @param categories
     *            the collection of categories to be set
     */
    public void setCategories(Collection<Category> categories) {
        this.categories = categories;
        dataProvider = new ListDataProvider<Category>(
                new ArrayList<>(categories)) {
            @Override
            public Object getId(Category item) {
                return item.getId();
            }
        };
        categoriesListing.setDataProvider(dataProvider);
        categoriesListing.setHeightByRows(categories.size());
    }

    private Component createCategoryEditor(Category category) {
        var nameField = new TextField();
        nameField.setValueChangeMode(ValueChangeMode.LAZY);
        nameField.setValueChangeTimeout(1000);
        nameField.setWidthFull();
        nameField.addShortcutListener(
                new ShortcutListener("Cancel", KeyCode.ESCAPE, null) {
                    @Override
                    public void handleAction(Object sender, Object target) {
                        presenter.requestUpdateCategories();
                        newCategoryButton.setEnabled(true);
                    }

                });
        if (category.getId() < 0) {
            nameField.focus();
        }

        var deleteButton = new Button(VaadinIcons.TRASH, event -> {
            var dialog = new ConfirmDialog(
                    getTranslation(I18n.WILL_DELETE, category.getName()),
                    ConfirmDialog.Type.ALERT);
            dialog.setConfirmText(getTranslation(I18n.DELETE));
            dialog.setCancelText(getTranslation(I18n.CANCEL));
            dialog.open();
            dialog.addConfirmedListener(e -> {
                presenter.removeCategory(category);
                dataProvider.getItems().remove(category);
                dataProvider.refreshAll();
                categoriesListing
                        .setHeightByRows(dataProvider.getItems().size());
                Notification.show(getTranslation(I18n.Category.CATEGORY_DELETED,
                        category.getName()));
            });
        });
        deleteButton.addStyleName(ValoTheme.BUTTON_DANGER);
        deleteButton.setDescription(getTranslation(I18n.DELETE));

        var binder = new BeanValidationBinder<>(Category.class);
        binder.forField(nameField).withValidator(value -> categories.stream()
                .filter(item -> item.getName().equals(value)).count() == 0,
                getTranslation(I18n.Category.DUPLICATE)).bind("name");
        binder.setBean(category);
        binder.addValueChangeListener(event -> {
            if (binder.isValid()) {
                try {
                    var saved = presenter.updateCategory(category);
                    if (category.getId() == -1) {
                        dataProvider.getItems().remove(category);
                        dataProvider.getItems().add(saved);
                        dataProvider.refreshAll();
                        nameField.focus();
                    } else {
                        dataProvider.getItems().remove(category);
                        dataProvider.getItems().add(saved);
                        dataProvider.refreshItem(saved);
                    }
                    presenter.requestUpdateCategories();
                    deleteButton.setEnabled(true);
                    newCategoryButton.setEnabled(true);
                    Notification.show(getTranslation(
                            I18n.Category.CATEGORY_SAVED, saved.getName()));
                } catch (OptimisticLockException | IllegalStateException e) {
                    Notification.show(getTranslation(I18n.SAVE_CONFLICT),
                            Notification.Type.WARNING_MESSAGE);
                    presenter.requestUpdateCategories();
                    newCategoryButton.setEnabled(true);
                }
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
