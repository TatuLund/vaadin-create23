package org.vaadin.tatu.vaadincreate.admin;

import java.io.Serializable;

import javax.persistence.OptimisticLockException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.tatu.vaadincreate.VaadinCreateUI;
import org.vaadin.tatu.vaadincreate.auth.AccessControl;
import org.vaadin.tatu.vaadincreate.backend.ProductDataService;
import org.vaadin.tatu.vaadincreate.backend.data.Category;
import org.vaadin.tatu.vaadincreate.eventbus.EventBus;

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
        try {
            getService().deleteCategory(category.getId());
            getEventBus().post(new CategoriesUpdatedEvent(category,
                    CategoriesUpdatedEvent.CategoryChange.DELETE));
            view.showDeleted(category.getName());
        } catch (IllegalArgumentException e) {
            view.showDeleteError();
        }
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
        Category newCat = null;
        try {
            newCat = getService().updateCategory(category);
            logger.info("Category '{}' updated.", category.getName());
            getEventBus().post(new CategoriesUpdatedEvent(newCat,
                    CategoriesUpdatedEvent.CategoryChange.SAVE));

        } catch (OptimisticLockException | IllegalStateException
                | IllegalArgumentException e) {
            requestUpdateCategories();
            view.showSaveConflict();
        }
        return newCat;
    }

    private EventBus getEventBus() {
        return EventBus.get();
    }

    private ProductDataService getService() {
        return VaadinCreateUI.get().getProductService();
    }

    public record CategoriesUpdatedEvent(Category category,
            CategoryChange change) {
        public enum CategoryChange {
            SAVE, DELETE
        }
    }

    private static Logger logger = LoggerFactory
            .getLogger(CategoryManagementPresenter.class);
}
