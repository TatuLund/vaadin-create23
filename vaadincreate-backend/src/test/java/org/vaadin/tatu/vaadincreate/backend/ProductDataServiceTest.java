package org.vaadin.tatu.vaadincreate.backend;

import org.junit.Before;
import org.junit.Test;
import org.vaadin.tatu.vaadincreate.backend.data.Category;
import org.vaadin.tatu.vaadincreate.backend.data.Product;
import org.vaadin.tatu.vaadincreate.backend.mock.MockProductDataService;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
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
    public void setUp() throws Exception {
        service = MockProductDataService.getInstance();
    }

    @Test
    public void canFetchProducts() throws Exception {
        assertFalse(service.getAllProducts().isEmpty());
    }

    @Test
    public void canFetchCategories() throws Exception {
        assertFalse(service.getAllCategories().isEmpty());
    }

    @Test
    public void updateTheProduct() throws Exception {
        var oldSize = service.getAllProducts().size();
        var p = service.getAllProducts().iterator().next();
        var version = p.getVersion();
        p.setProductName("My Test Name");
        service.updateProduct(p);
        var p2 = service.getProductById(p.getId());
        assertEquals(version + 1, p2.getVersion());
        assertEquals("My Test Name", p2.getProductName());
        assertEquals(oldSize, service.getAllProducts().size());
    }

    @Test
    public void addNewProduct() throws Exception {
        var oldSize = service.getAllProducts().size();
        Product p = new Product();
        p.setProductName("A new book");
        p.setPrice(new BigDecimal(10));
        assertEquals(-1, p.getId());
        var newProduct = service.updateProduct(p);
        assertEquals(0, newProduct.getVersion());
        assertNotEquals(-1, newProduct.getId());
        assertEquals(oldSize + 1, service.getAllProducts().size());

        var foundProduct = service.getProductById(newProduct.getId());
        assertTrue(foundProduct.equals(newProduct));
    }

    @Test(expected = IllegalArgumentException.class)
    public void updateNonExistentProduct() {
        Product p = new Product();
        p.setProductName("A new book");
        p.setPrice(new BigDecimal(10));
        p.setId(1000);
        service.updateProduct(p);
    }

    @Test
    public void removeProduct() throws Exception {
        var oldSize = service.getAllProducts().size();
        var p = service.getAllProducts().iterator().next();
        var pid = p.getId();
        service.deleteProduct(pid);
        assertEquals(null, service.getProductById(pid));
        assertEquals(oldSize - 1, service.getAllProducts().size());
    }

    @Test
    public void findProductById() {
        assertNotEquals(null, service.getProductById(1));
    }

    @Test
    public void findProductByNonExistentId() {
        assertEquals(null, service.getProductById(1000));
    }

    @Test(expected = IllegalArgumentException.class)
    public void removeProductByNonExistentId() {
        service.deleteProduct(1000);
    }

    @Test(expected = OptimisticLockException.class)
    public void optimisticLocking() {
        var product = service.getProductById(1);
        service.updateProduct(product);
        service.updateProduct(product);
    }

    @Test(expected = OptimisticLockException.class)
    public void optimisticLockingCategory() {
        var category = service.getAllCategories().stream().skip(2).findFirst()
                .get();
        service.updateCategory(category);
        service.updateCategory(category);
    }

    @Test
    public void addUpdateRemoveCategory() {
        var category = new Category();
        category.setName("Sports books");
        var newCategory = service.updateCategory(category);
        assertFalse(category.equals(newCategory));
        assertTrue(newCategory.getId() > 0);
        assertEquals("Sports books", newCategory.getName());
        assertFalse(category == newCategory);
        assertEquals(0, newCategory.getVersion());
        assertTrue(service.getAllCategories().contains(newCategory));

        newCategory.setName("Athletics");
        var updatedCategory = service.updateCategory(newCategory);
        assertEquals(1, updatedCategory.getVersion());
        assertTrue(updatedCategory.equals(newCategory));
        assertFalse(updatedCategory == newCategory);
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
        var foundBook = service.getProductById(bookId);
        assertEquals("Athletics",
                foundBook.getCategory().stream().findFirst().get().getName());

        service.deleteCategory(newCategory.getId());
        foundBook = service.getProductById(bookId);
        assertTrue(foundBook.getCategory().isEmpty());
    }

    @Test(expected = IllegalArgumentException.class)
    public void updateNonExistingCategoryThrows() {
        var category = new Category();
        category.setName("Sports");
        category.setId(20);
        service.updateCategory(category);
    }

    @Test
    public void drafts() {
        var userName = "user";
        var draft = service.findDraft(userName);
        assertNull(draft);
        var product = new Product();
        service.saveDraft(userName, product);
        draft = service.findDraft(userName);
        assertEquals(product, draft);
        assertFalse(product == draft);
        service.saveDraft(userName, null);
        draft = service.findDraft(userName);
        assertNull(draft);
    }
}
