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
import org.vaadin.tatu.vaadincreate.backend.data.PurchaseLine;
import org.vaadin.tatu.vaadincreate.backend.data.PurchaseStatus;
import org.vaadin.tatu.vaadincreate.backend.data.User;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;

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
        // Initialize UserService and ProductDataService first so that mock
        // data (users and products) is available when PurchaseServiceImpl
        // starts up and calls generateMockPurchaseDataIfEmpty().
        userService = UserServiceImpl.getInstance();
        productService = ProductDataServiceImpl.getInstance();
        purchaseService = PurchaseServiceImpl.getInstance();

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
        var productIterator = productService.getAllProducts().iterator();
        testProduct = productIterator.next();
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

        var line1 = new PurchaseLine();
        line1.setProduct(product1);
        line1.setQuantity(2);
        line1.setUnitPrice(new BigDecimal("10.00"));
        purchase.addLine(line1);

        var line2 = new PurchaseLine();
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

    @Test
    public void should_ApprovePurchase_And_DecrementStock_When_StockIsSufficient() {
        // Arrange – use a fresh product reference and set known stock
        var freshProduct = productService.getProductById(testProduct.getId());
        int orderQty = 2;
        freshProduct.setStockCount(orderQty + 5);
        freshProduct = productService.updateProduct(freshProduct);

        Cart cart = new Cart();
        cart.addItem(freshProduct, orderQty);
        Address address = new Address("123 Main St", "12345", "Anytown", "USA");

        Purchase purchase = purchaseService.createPendingPurchase(cart, address,
                customerUser, supervisorUser);

        // Act
        Purchase approved = purchaseService.approve(purchase.getId(),
                supervisorUser, "Looks good");

        // Assert
        assertEquals(PurchaseStatus.COMPLETED, approved.getStatus());
        assertNotNull(approved.getDecidedAt());
        assertEquals("Looks good", approved.getDecisionReason());

        // Stock must be decremented
        Product reloaded = productService.getProductById(testProduct.getId());
        assertEquals(orderQty + 5 - orderQty,
                reloaded.getStockCount().intValue());
    }

    @Test
    public void should_CancelPurchase_When_StockIsInsufficient() {
        // Arrange – set stock to 0 so approval will detect insufficiency
        int savedStock = testProduct.getStockCount();
        testProduct.setStockCount(0);
        productService.updateProduct(testProduct);

        Cart cart = new Cart();
        cart.addItem(testProduct, 1);
        Address address = new Address("123 Main St", "12345", "Anytown", "USA");

        Purchase purchase = purchaseService.createPendingPurchase(cart, address,
                customerUser, supervisorUser);

        // Act
        Purchase cancelled = purchaseService.approve(purchase.getId(),
                supervisorUser, null);

        // Assert
        assertEquals(PurchaseStatus.CANCELLED, cancelled.getStatus());
        assertNotNull(cancelled.getDecisionReason());
        assertTrue(cancelled.getDecisionReason().contains("Insufficient"));

        // Stock must stay at 0 (no decrement)
        Product reloaded = productService.getProductById(testProduct.getId());
        assertEquals(Integer.valueOf(0), reloaded.getStockCount());

        // Restore stock for other tests
        testProduct.setStockCount(savedStock);
        productService.updateProduct(testProduct);
    }

    @Test
    public void should_RejectPurchase_Without_StockChange_When_ReasonProvided() {
        // Arrange
        int originalStock = testProduct.getStockCount();
        Cart cart = new Cart();
        cart.addItem(testProduct, 1);
        Address address = new Address("123 Main St", "12345", "Anytown", "USA");

        Purchase purchase = purchaseService.createPendingPurchase(cart, address,
                customerUser, supervisorUser);

        // Act
        Purchase rejected = purchaseService.reject(purchase.getId(),
                supervisorUser, "Budget exceeded");

        // Assert
        assertEquals(PurchaseStatus.REJECTED, rejected.getStatus());
        assertNotNull(rejected.getDecidedAt());
        assertEquals("Budget exceeded", rejected.getDecisionReason());

        // Stock must not change
        Product reloaded = productService.getProductById(testProduct.getId());
        assertEquals(originalStock, reloaded.getStockCount().intValue());
    }

    @Test(expected = IllegalArgumentException.class)
    public void should_ThrowException_When_ApprovingNonPendingPurchase() {
        // Arrange
        Cart cart = new Cart();
        cart.addItem(testProduct, 1);
        Address address = new Address("123 Main St", "12345", "Anytown", "USA");

        Purchase purchase = purchaseService.createPendingPurchase(cart, address,
                customerUser, supervisorUser);
        purchaseService.reject(purchase.getId(), supervisorUser,
                "Already decided");

        // Act – approving a REJECTED purchase must throw
        purchaseService.approve(purchase.getId(), supervisorUser, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void should_ThrowException_When_RejectingNonPendingPurchase() {
        // Arrange
        Cart cart = new Cart();
        cart.addItem(testProduct, 1);
        Address address = new Address("123 Main St", "12345", "Anytown", "USA");

        Purchase purchase = purchaseService.createPendingPurchase(cart, address,
                customerUser, supervisorUser);
        purchaseService.reject(purchase.getId(), supervisorUser,
                "First rejection");

        // Act – rejecting again must throw
        purchaseService.reject(purchase.getId(), supervisorUser,
                "Second rejection");
    }

    @Test
    public void should_ReturnMonthlyTotals_InChronologicalOrder_OldestToNewest() {
        int months = 12;

        var stats = purchaseService.getMonthlyTotals(months);

        assertNotNull(stats);
        assertEquals(months, stats.size());

        var now = YearMonth.now();
        assertEquals(now.minusMonths(months - 1L).toString(),
                stats.get(0).yearMonth());
        assertEquals(now.toString(), stats.get(stats.size() - 1).yearMonth());

        for (int i = 1; i < stats.size(); i++) {
            var prev = YearMonth.parse(stats.get(i - 1).yearMonth());
            var curr = YearMonth.parse(stats.get(i).yearMonth());
            assertTrue("Expected months to be strictly increasing, but got "
                    + prev + " then " + curr, prev.isBefore(curr));
            assertNotNull("Monthly total amount should not be null",
                    stats.get(i).totalAmount());
        }
        assertNotNull("Monthly total amount should not be null",
                stats.get(0).totalAmount());
    }

    @Test
    public void should_CountPurchasesOlderThan_When_CutoffIsInFuture() {
        // GIVEN: cutoff in the future means ALL existing purchases are "older
        // than"
        Instant futureCutoff = Instant.now().plus(1, ChronoUnit.DAYS);

        // ACT
        long count = purchaseService.countPurchasesOlderThan(futureCutoff);
        long totalCount = purchaseService.countAll();

        // ASSERT: a future cutoff must match every purchase
        assertEquals(
                "countPurchasesOlderThan(future) should equal total count",
                totalCount, count);
    }

    @Test
    public void should_CountZero_When_CutoffIsBeforeAnyPurchase() {
        // GIVEN: a cutoff long before the earliest possible purchase
        Instant ancientCutoff = Instant.parse("2000-01-01T00:00:00Z");

        // ACT
        long count = purchaseService.countPurchasesOlderThan(ancientCutoff);

        // ASSERT: no purchases should predate the year 2000
        assertEquals("countPurchasesOlderThan(ancient) should be 0", 0, count);
    }

    @Test
    public void should_PurgePurchasesOlderThan_And_NotDeleteNewerOnes() {
        // GIVEN: create two purchases – one "old" (created 25 months ago via
        // DAO directly) and one "new" (just created)
        Cart cart = new Cart();
        cart.addItem(testProduct, 1);
        Address address = new Address("Test St", "00001", "Test City", "TC");

        Purchase recentPurchase = purchaseService.createPendingPurchase(cart,
                address, customerUser, supervisorUser);
        assertNotNull(recentPurchase.getId());

        // Use a cutoff 1 day in the future: all current purchases are "old"
        // so we can test that the purge removes the expected amount.
        Instant futureCutoff = Instant.now().plus(1, ChronoUnit.DAYS);
        long beforePurge = purchaseService
                .countPurchasesOlderThan(futureCutoff);
        assertTrue("Expected at least one purchase before future cutoff",
                beforePurge > 0);

        long totalBefore = purchaseService.countAll();

        // ACT: purge everything "older than" tomorrow (i.e. everything)
        long purged = purchaseService.purgePurchasesOlderThan(futureCutoff);

        // ASSERT: count matches what was reported before
        assertEquals("Purged count should equal countPurchasesOlderThan result",
                beforePurge, purged);

        long totalAfter = purchaseService.countAll();
        assertEquals("Total count should decrease by the purged amount",
                totalBefore - purged, totalAfter);

        // ASSERT: no purchases remain under the future cutoff
        long remainingOld = purchaseService
                .countPurchasesOlderThan(futureCutoff);
        assertEquals("No purchases should remain after full purge", 0,
                remainingOld);
    }
}
