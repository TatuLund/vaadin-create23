package org.vaadin.tatu.vaadincreate.backend.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;

import org.junit.Test;

public class PurchaseTest {

    @Test
    public void should_MaintainLineBackReference_When_AddingAndRemovingLine() {
        User requester = new User(1, "customer1", "secret", User.Role.CUSTOMER);
        Address address = new Address("Main St 1", "00100", "Helsinki", "FI");
        Purchase purchase = new Purchase(requester, address);

        Product product = new Product();
        product.setProductName("Book");
        product.setPrice(new BigDecimal("12.50"));
        product.setStockCount(10);
        product.setAvailability(Availability.AVAILABLE);

        PurchaseLine line = new PurchaseLine();
        line.setProduct(product);
        line.setQuantity(2);
        line.setUnitPrice(new BigDecimal("12.50"));

        purchase.addLine(line);
        assertEquals(purchase, line.getPurchase());
        assertEquals(1, purchase.getLines().size());

        purchase.removeLine(line);

        assertTrue(purchase.getLines().isEmpty());
        assertNull(line.getPurchase());
    }
}