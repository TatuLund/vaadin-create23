package org.vaadin.tatu.vaadincreate.backend.service;

import org.junit.Before;
import org.junit.Test;
import org.vaadin.tatu.vaadincreate.backend.ProductDataService;
import org.vaadin.tatu.vaadincreate.backend.PurchaseService;
import org.vaadin.tatu.vaadincreate.backend.UserService;
import org.vaadin.tatu.vaadincreate.backend.data.Address;
import org.vaadin.tatu.vaadincreate.backend.data.Cart;
import org.vaadin.tatu.vaadincreate.backend.data.Product;
import org.vaadin.tatu.vaadincreate.backend.data.Purchase;
import org.vaadin.tatu.vaadincreate.backend.data.PurchaseStatus;
import org.vaadin.tatu.vaadincreate.backend.data.User;
import org.vaadin.tatu.vaadincreate.backend.data.UserSupervisor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;

/**
 * Test class for {@link PurchaseService}.
 *
 * These integration tests run against the in-memory H2 database.
 */
@SuppressWarnings("null")
public class PurchaseServiceTest {

    private PurchaseService purchaseService;
    private UserService userService;
    private ProductDataService productService;
    private User customerUser;
    private User supervisorUser;
    private Product testProduct;

    @Before
    public void setUp() {
        purchaseService = PurchaseServiceImpl.getInstance();
        userService = UserServiceImpl.getInstance();
        productService = ProductDataServiceImpl.getInstance();

        // Use existing users or get first users by role
        // Try to find existing users with CUSTOMER and USER roles
        var allUsers = userService.getAllUsers();

        // Find e a customer user
        customerUser = allUsers.stream()
                .filter(u -> u.getRole() == User.Role.CUSTOMER).findFirst()
                .get();

        // Find a supervisor user
        supervisorUser = allUsers.stream()
                .filter(u -> u.getRole() == User.Role.USER).findFirst().get();

        // Get a test product
        testProduct = productService.getAllProducts().iterator().next();
    }

    @Test
    public void should_CreatePendingPurchase_When_ValidCartAndAddress() {
        // Arrange
        Cart cart = new Cart();
        cart.addItem(testProduct, 2);

        Address address = new Address("123 Main St", "12345", "Anytown", "USA");

        // Act
        Purchase purchase = purchaseService.createPendingPurchase(cart, address,
                customerUser, supervisorUser);

        // Assert
        assertNotNull(purchase);
        assertNotNull(purchase.getId());
        assertEquals(PurchaseStatus.PENDING, purchase.getStatus());
        assertEquals(customerUser.getId(), purchase.getRequester().getId());
        assertEquals(supervisorUser.getId(), purchase.getApprover().getId());
        assertNotNull(purchase.getCreatedAt());
        assertNull(purchase.getDecidedAt());

        // Verify address snapshot
        assertEquals("123 Main St", purchase.getDeliveryAddress().getStreet());
        assertEquals("12345", purchase.getDeliveryAddress().getPostalCode());
        assertEquals("Anytown", purchase.getDeliveryAddress().getCity());
        assertEquals("USA", purchase.getDeliveryAddress().getCountry());

        // Verify purchase lines
        assertEquals(1, purchase.getLines().size());
        var line = purchase.getLines().get(0);
        assertEquals(testProduct.getId(), line.getProduct().getId());
        assertEquals(Integer.valueOf(2), line.getQuantity());
        assertEquals(testProduct.getPrice(), line.getUnitPrice());

        // Verify derived total
        BigDecimal expectedLineTotal = testProduct.getPrice()
                .multiply(BigDecimal.valueOf(2));
        assertEquals(expectedLineTotal, line.getLineTotal());
        assertEquals(expectedLineTotal, purchase.getTotalAmount());
    }

    @Test
    public void should_SnapshotProductPrice_When_CreatingPurchase() {
        // Arrange
        Cart cart = new Cart();
        cart.addItem(testProduct, 1);

        Address address = new Address("123 Main St", "12345", "Anytown", "USA");
        BigDecimal originalPrice = testProduct.getPrice();

        // Act
        Purchase purchase = purchaseService.createPendingPurchase(cart, address,
                customerUser, supervisorUser);

        // Now change the product price
        testProduct.setPrice(originalPrice.multiply(BigDecimal.valueOf(2)));
        productService.updateProduct(testProduct);

        // Assert - purchase line should still have the original price
        var line = purchase.getLines().get(0);
        assertEquals(originalPrice, line.getUnitPrice());
    }

    @Test
    public void should_NotModifyStock_When_CreatingPurchase() {
        // Arrange
        Cart cart = new Cart();
        cart.addItem(testProduct, 2);

        Address address = new Address("123 Main St", "12345", "Anytown", "USA");
        Integer originalStock = testProduct.getStockCount();

        // Act
        purchaseService.createPendingPurchase(cart, address, customerUser,
                supervisorUser);

        // Assert - stock should remain unchanged
        Product reloadedProduct = productService
                .getProductById(testProduct.getId());
        assertEquals(originalStock, reloadedProduct.getStockCount());
    }

    @Test(expected = IllegalArgumentException.class)
    public void should_ThrowException_When_CartIsEmpty() {
        // Arrange
        Cart emptyCart = new Cart();
        Address address = new Address("123 Main St", "12345", "Anytown", "USA");

        // Act
        purchaseService.createPendingPurchase(emptyCart, address, customerUser,
                supervisorUser);
    }

    @Test
    public void should_CreateMultiplePurchaseLines_When_CartHasMultipleProducts() {
        // Arrange
        Cart cart = new Cart();
        var products = productService.getAllProducts();
        var iter = products.iterator();
        Product product1 = iter.next();
        Product product2 = iter.next();

        cart.addItem(product1, 1);
        cart.addItem(product2, 3);

        Address address = new Address("123 Main St", "12345", "Anytown", "USA");

        // Act
        Purchase purchase = purchaseService.createPendingPurchase(cart, address,
                customerUser, supervisorUser);

        // Assert
        assertEquals(2, purchase.getLines().size());

        BigDecimal expectedTotal = product1.getPrice()
                .add(product2.getPrice().multiply(BigDecimal.valueOf(3)));
        assertEquals(expectedTotal, purchase.getTotalAmount());
    }

    @Test
    public void should_FindMyPurchases_When_PurchasesExist() {
        // Arrange
        Cart cart = new Cart();
        cart.addItem(testProduct, 1);
        Address address = new Address("123 Main St", "12345", "Anytown", "USA");

        purchaseService.createPendingPurchase(cart, address, customerUser,
                supervisorUser);
        purchaseService.createPendingPurchase(cart, address, customerUser,
                supervisorUser);

        // Act
        var purchases = purchaseService.findMyPurchases(customerUser, 0, 10);
        long count = purchaseService.countMyPurchases(customerUser);

        // Assert
        assertFalse(purchases.isEmpty());
        assertTrue(purchases.size() >= 2);
        assertEquals(purchases.size(), count);

        // Verify all purchases belong to the customer
        for (Purchase p : purchases) {
            assertEquals(customerUser.getId(), p.getRequester().getId());
            // Regression: UI renders totals outside Hibernate session.
            assertNotNull(p.getTotalAmount());
        }
    }

    @Test
    public void should_FindPendingForApprover_When_PendingPurchasesExist() {
        // Arrange
        Cart cart = new Cart();
        cart.addItem(testProduct, 1);
        Address address = new Address("123 Main St", "12345", "Anytown", "USA");

        purchaseService.createPendingPurchase(cart, address, customerUser,
                supervisorUser);

        // Act
        var pendingPurchases = purchaseService
                .findPendingForApprover(supervisorUser, 0, 10);
        long count = purchaseService.countPendingForApprover(supervisorUser);

        // Assert
        assertFalse(pendingPurchases.isEmpty());
        assertTrue(count > 0);

        // Verify all purchases are pending and assigned to supervisor
        for (Purchase p : pendingPurchases) {
            assertEquals(PurchaseStatus.PENDING, p.getStatus());
            assertEquals(supervisorUser.getId(), p.getApprover().getId());
        }
    }

    @Test
    public void should_FindAllPurchases_When_PurchasesExist() {
        // Arrange
        Cart cart = new Cart();
        cart.addItem(testProduct, 1);
        Address address = new Address("123 Main St", "12345", "Anytown", "USA");

        long initialCount = purchaseService.countAll();

        purchaseService.createPendingPurchase(cart, address, customerUser,
                supervisorUser);

        // Act
        var allPurchases = purchaseService.findAll(0, 100);
        long count = purchaseService.countAll();

        // Assert
        assertFalse(allPurchases.isEmpty());
        assertEquals(initialCount + 1, count);
    }

    @Test
    public void should_CalculateTotalCorrectly_When_PurchaseHasLines() {
        // Arrange
        Purchase purchase = new Purchase();
        purchase.setRequester(customerUser);
        purchase.setDeliveryAddress(
                new Address("123 Main St", "12345", "Anytown", "USA"));

        var products = productService.getAllProducts();
        var iter = products.iterator();
        Product product1 = iter.next();
        Product product2 = iter.next();

        var line1 = new org.vaadin.tatu.vaadincreate.backend.data.PurchaseLine();
        line1.setProduct(product1);
        line1.setQuantity(2);
        line1.setUnitPrice(new BigDecimal("10.00"));
        purchase.addLine(line1);

        var line2 = new org.vaadin.tatu.vaadincreate.backend.data.PurchaseLine();
        line2.setProduct(product2);
        line2.setQuantity(3);
        line2.setUnitPrice(new BigDecimal("5.50"));
        purchase.addLine(line2);

        // Act
        BigDecimal total = purchase.getTotalAmount();

        // Assert
        // (2 * 10.00) + (3 * 5.50) = 20.00 + 16.50 = 36.50
        assertEquals(new BigDecimal("36.50"), total);
    }

    @Test
    public void should_PaginatePurchases_When_MultipleExist() {
        // Arrange
        Cart cart = new Cart();
        cart.addItem(testProduct, 1);
        Address address = new Address("123 Main St", "12345", "Anytown", "USA");

        // Create 5 purchases
        for (int i = 0; i < 5; i++) {
            purchaseService.createPendingPurchase(cart, address, customerUser,
                    supervisorUser);
        }

        // Act
        var page1 = purchaseService.findMyPurchases(customerUser, 0, 2);
        var page2 = purchaseService.findMyPurchases(customerUser, 2, 2);

        // Assert
        assertEquals(2, page1.size());
        assertEquals(2, page2.size());
        assertNotEquals(page1.get(0).getId(), page2.get(0).getId());
    }
}
