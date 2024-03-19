package org.vaadin.tatu.vaadincreate.admin;

import java.io.Serializable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.tatu.vaadincreate.backend.ProductDataService;
import org.vaadin.tatu.vaadincreate.backend.data.Category;

@SuppressWarnings("serial")
public class CategoryManagementPresenter implements Serializable {

    private CategoryManagementView view;

    public CategoryManagementPresenter(CategoryManagementView view) {
        this.view = view;
    }

    public void requestUpdateCategories() {
        logger.info("Fetching categories");
        view.setCategories(ProductDataService.get().getAllCategories());
    }

    public void removeCategory(Category category) {
        ProductDataService.get().deleteCategory(category.getId());
        logger.info("Category '{}' removed.", category.getName());
    }

    public Category updateCategory(Category category) {
        var newCat = ProductDataService.get().updateCategory(category);
        logger.info("Category '{}' updated.", category.getName());
        return newCat;
    }

    private Logger logger = LoggerFactory.getLogger(this.getClass());
}
