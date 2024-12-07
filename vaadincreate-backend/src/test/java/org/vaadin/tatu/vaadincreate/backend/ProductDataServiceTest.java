package org.vaadin.tatu.vaadincreate.backend;

import org.junit.Before;
import org.junit.Test;
import org.vaadin.tatu.vaadincreate.backend.data.Category;
import org.vaadin.tatu.vaadincreate.backend.data.Product;
import org.vaadin.tatu.vaadincreate.backend.service.ProductDataServiceImpl;
import org.vaadin.tatu.vaadincreate.backend.service.UserServiceImpl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.util.Set;

import javax.persistence.OptimisticLockException;

/**
 * Simple unit test for the back-end data service.
 */
public class ProductDataServiceTest {

    private ProductDataService service;

    @Before
    public void setUp() {
        service = ProductDataServiceImpl.getInstance();
    }

    @Test
    public void canFetchProducts() {
        assertFalse(service.getAllProducts().isEmpty());
    }

    @Test
    public void canFetchCategories() {
        assertFalse(service.getAllCategories().isEmpty());
    }

    @Test
    public void updateTheProduct() {
        var oldSize = service.getAllProducts().size();
        var p = service.getAllProducts().iterator().next();
        var version = p.getVersion();
        p.setProductName("My Test Name");
        service.updateProduct(p);
        var p2 = service.getProductById(p.getId());
        assertEquals(Integer.valueOf(version + 1), p2.getVersion());
        assertEquals("My Test Name", p2.getProductName());
        assertEquals(oldSize, service.getAllProducts().size());
    }

    @Test
    public void addNewProduct() {
        var oldSize = service.getAllProducts().size();
        Product p = new Product();
        p.setProductName("A new book");
        p.setPrice(new BigDecimal(10));
        assertNull(p.getId());
        var newProduct = service.updateProduct(p);
        assertEquals(Integer.valueOf(0), newProduct.getVersion());
        assertNotEquals(Integer.valueOf(-1), newProduct.getId());
        assertEquals(oldSize + 1, service.getAllProducts().size());

        var foundProduct = service.getProductById(newProduct.getId());
        assertEquals(foundProduct, newProduct);
    }

    @Test
    public void removeProduct() {
        var oldSize = service.getAllProducts().size();
        var p = service.getAllProducts().iterator().next();
        var pid = p.getId();
        service.deleteProduct(pid);
        assertEquals(null, service.getProductById(pid));
        assertEquals(oldSize - 1, service.getAllProducts().size());
    }

    @Test
    public void findProductById() {
        var id = service.getAllProducts().stream().findFirst().get().getId();
        assertNotNull(service.getProductById(id));
    }

    @Test
    public void findProductByNonExistentId() {
        assertNull(service.getProductById(10000));
    }

    @Test(expected = IllegalArgumentException.class)
    public void removeProductByNonExistentId() {
        service.deleteProduct(1000);
    }

    @Test
    public void optimisticLocking() {
        var id = service.getAllProducts().stream().findFirst().get().getId();
        var product = service.getProductById(id);
        var copy = new Product(product);
        service.updateProduct(product);
        assertThrows(OptimisticLockException.class,
                () -> service.updateProduct(copy));
    }

    @Test
    public void optimisticLockingCategory() {
        var category = service.getAllCategories().stream().skip(2).findFirst()
                .get();
        var copy = new Category(category);
        var updated = service.updateCategory(category);
        assertNotEquals(updated.getVersion(), copy.getVersion());
        assertThrows(OptimisticLockException.class,
                () -> service.updateCategory(copy));
    }

    @Test
    public void addUpdateRemoveCategory() {
        var category = new Category();
        category.setName("Sports books");
        var newCategory = service.updateCategory(category);
        assertTrue(newCategory.getId() > 0);
        assertEquals("Sports books", newCategory.getName());
        assertEquals(Integer.valueOf(0), newCategory.getVersion());
        assertTrue(service.getAllCategories().contains(newCategory));

        newCategory.setName("Athletics");
        var updatedCategory = service.updateCategory(newCategory);
        assertEquals(Integer.valueOf(1), updatedCategory.getVersion());
        assertEquals(updatedCategory, newCategory);
        assertTrue(service.getAllCategories().contains(updatedCategory));
        assertEquals("Athletics", updatedCategory.getName());

        service.deleteCategory(updatedCategory.getId());
        assertFalse(service.getAllCategories().contains(updatedCategory));
    }

    @Test
    public void updateCategoryUsedInProduct_removeUsedCategory() {
        var category = new Category();
        category.setName("Sports");
        var newCategory = service.updateCategory(category);

        var book = new Product();
        book.setProductName("Sports book");
        book.setCategory(Set.of(newCategory));

        var newBook = service.updateProduct(book);
        var bookId = newBook.getId();

        newCategory.setName("Athletics");
        service.updateCategory(newCategory);

        var foundBook = service.getProductById(bookId);
        assertEquals("Athletics",
                foundBook.getCategory().stream().findFirst().get().getName());

        service.deleteCategory(newCategory.getId());
        foundBook = service.getProductById(bookId);
        assertTrue(foundBook.getCategory().isEmpty());
    }

    @Test(expected = IllegalArgumentException.class)
    public void saveDuplicateCategoryThrows() {
        var category = new Category();
        category.setName("Duplicate");
        service.updateCategory(category);

        category = new Category();
        category.setName("Duplicate");
        service.updateCategory(category);
    }

    @Test
    public void drafts() {
        var userService = UserServiceImpl.getInstance();
        var user = userService.findByName("Admin").get();

        var draft = service.findDraft(user);
        assertNull(draft);
        var product = new Product();
        service.saveDraft(user, product);
        draft = service.findDraft(user);
        assertNotSame(product, draft);
        service.saveDraft(user, null);
        draft = service.findDraft(user);
        assertNull(draft);
    }
}
