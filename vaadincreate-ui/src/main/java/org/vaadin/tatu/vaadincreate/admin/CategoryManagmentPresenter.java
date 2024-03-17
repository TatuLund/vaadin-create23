package org.vaadin.tatu.vaadincreate.admin;

import java.io.Serializable;

import org.vaadin.tatu.vaadincreate.backend.ProductDataService;
import org.vaadin.tatu.vaadincreate.backend.data.Category;

@SuppressWarnings("serial")
public class CategoryManagmentPresenter implements Serializable {

    private CategoryManagementView view;

    public CategoryManagmentPresenter(CategoryManagementView view) {
        this.view = view;
    }

    public void requestUpdateCategories() {
        view.setCategories(ProductDataService.get().getAllCategories());
    }

    public void removeCategory(Category category) {
        ProductDataService.get().deleteCategory(category.getId());
    }

    public Category updateCategory(Category category) {
        return ProductDataService.get().updateCategory(category);
    }
}
