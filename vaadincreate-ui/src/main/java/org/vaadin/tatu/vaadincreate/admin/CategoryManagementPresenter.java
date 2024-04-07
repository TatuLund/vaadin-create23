package org.vaadin.tatu.vaadincreate.admin;

import java.io.Serializable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.tatu.vaadincreate.VaadinCreateUI;
import org.vaadin.tatu.vaadincreate.backend.ProductDataService;
import org.vaadin.tatu.vaadincreate.backend.data.Category;

@SuppressWarnings("serial")
public class CategoryManagementPresenter implements Serializable {

    private CategoryManagementView view;
    private ProductDataService service = VaadinCreateUI.get()
            .getProductService();

    public CategoryManagementPresenter(CategoryManagementView view) {
        this.view = view;
    }

    public void requestUpdateCategories() {
        logger.info("Fetching categories");
        view.setCategories(service.getAllCategories());
    }

    public void removeCategory(Category category) {
        service.deleteCategory(category.getId());
        logger.info("Category '{}' removed.", category.getName());
    }

    public Category updateCategory(Category category) {
        var newCat = service.updateCategory(category);
        logger.info("Category '{}' updated.", category.getName());
        return newCat;
    }

    private Logger logger = LoggerFactory.getLogger(this.getClass());
}
