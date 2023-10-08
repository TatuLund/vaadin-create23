package org.vaadin.tatu.vaadincreate.backend;

import org.junit.Before;
import org.junit.Test;
import org.vaadin.tatu.vaadincreate.backend.data.Product;
import org.vaadin.tatu.vaadincreate.backend.mock.MockProductDataService;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;

import java.math.BigDecimal;

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
        p.setProductName("My Test Name");
        service.updateProduct(p);
        Product p2 = service.getAllProducts().iterator().next();
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
        assertNotEquals(-1, newProduct.getId());
        assertEquals(oldSize + 1, service.getAllProducts().size());
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
}
