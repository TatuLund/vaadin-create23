package org.vaadin.tatu.vaadincreate.admin;

import java.io.Serializable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.tatu.vaadincreate.VaadinCreateUI;
import org.vaadin.tatu.vaadincreate.auth.AccessControl;
import org.vaadin.tatu.vaadincreate.backend.ProductDataService;
import org.vaadin.tatu.vaadincreate.backend.data.Category;

@SuppressWarnings("serial")
public class CategoryManagementPresenter implements Serializable {

    private CategoryManagementView view;
    private AccessControl accessControl = VaadinCreateUI.get()
            .getAccessControl();

    public CategoryManagementPresenter(CategoryManagementView view) {
        this.view = view;
    }

    /**
     * Request all categories from the backend and update the view.
     */
    public void requestUpdateCategories() {
        logger.info("Fetching categories");
        view.setCategories(getService().getAllCategories());
    }

    /**
     * Remove a category from the backend.
     *
     * @param category
     *            the category to remove
     */
    public void removeCategory(Category category) {
        accessControl.assertAdmin();
        getService().deleteCategory(category.getId());
        logger.info("Category '{}' removed.", category.getName());
    }

    /**
     * Add a new category to the backend.
     *
     * @param category
     *            the category to add
     * @return the new category
     */
    public Category updateCategory(Category category) {
        accessControl.assertAdmin();
        var newCat = getService().updateCategory(category);
        logger.info("Category '{}' updated.", category.getName());
        return newCat;
    }

    private ProductDataService getService() {
        return VaadinCreateUI.get().getProductService();
    }

    private static Logger logger = LoggerFactory
            .getLogger(CategoryManagementPresenter.class);
}
