package org.vaadin.tatu.vaadincreate.backend.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import java.util.Objects;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.tatu.vaadincreate.backend.PurchaseHistoryMode;
import org.vaadin.tatu.vaadincreate.backend.PurchaseService;
import org.vaadin.tatu.vaadincreate.backend.dao.ProductDao;
import org.vaadin.tatu.vaadincreate.backend.dao.PurchaseDao;
import org.vaadin.tatu.vaadincreate.backend.dao.UserDao;
import org.vaadin.tatu.vaadincreate.backend.data.Address;
import org.vaadin.tatu.vaadincreate.backend.data.Cart;
import org.vaadin.tatu.vaadincreate.backend.data.Product;
import org.vaadin.tatu.vaadincreate.backend.data.Purchase;
import org.vaadin.tatu.vaadincreate.backend.data.PurchaseLine;
import org.vaadin.tatu.vaadincreate.backend.data.PurchaseStatus;
import org.vaadin.tatu.vaadincreate.backend.data.User;
import org.vaadin.tatu.vaadincreate.backend.mock.MockDataGenerator;

/**
 * Implementation of PurchaseService. This is a singleton service managing
 * purchase operations.
 */
@NullMarked
@SuppressWarnings("java:S6548")
public class PurchaseServiceImpl implements PurchaseService {

    @Nullable
    private static PurchaseServiceImpl instance;
    private final PurchaseDao purchaseDao;

    private PurchaseServiceImpl() {
        this.purchaseDao = new PurchaseDao();
        logger.info("PurchaseService initialized");

        // Optional: generate a large Purchase dataset for UX testing.
        // Disable with: -Dgenerate.data=false
        var env = System.getProperty("generate.data");
        if (env == null || env.equals("true")) {
            generateMockPurchaseDataIfEmpty();
            logger.info("Generated mock purchase data");
        }
    }

    private void generateMockPurchaseDataIfEmpty() {
        // Avoid duplicating data across restarts.
        if (purchaseDao.countAll() > 0) {
            logger.info(
                    "Skipping UX purchase data generation (purchases already exist)");
            return;
        }

        var userDao = new UserDao();
        var productDao = new ProductDao();

        var customers = IntStream.rangeClosed(11, 20)
                .mapToObj(i -> "Customer" + i)
                .map(name -> {
                    var u = userDao.findByName(name);
                    if (u == null) {
                        throw new IllegalStateException(
                                "Required user not found: " + name);
                    }
                    return u;
                }).toList();

        var approver5 = userDao.findByName("User5");
        var approver6 = userDao.findByName("User6");
        if (approver5 == null || approver6 == null) {
            throw new IllegalStateException(
                    "Required approvers not found: User5/User6");
        }
        var approvers = List.<User> of(approver5, approver6);

        var products = productDao.getAllProducts().stream().toList();
        if (products.isEmpty()) {
            throw new IllegalStateException(
                    "No products found for purchase lines");
        }
        var purchases = MockDataGenerator.createMockPurchases(customers,
                approvers, products);

        purchaseDao.savePurchases(purchases);

        logger.info("Generated {} UX Purchases", purchases.size());
    }

    /**
     * Gets the singleton instance of PurchaseServiceImpl.
     *
     * @return the singleton instance
     */
    @SuppressWarnings("null")
    public static synchronized PurchaseService getInstance() {
        if (instance == null) {
            instance = new PurchaseServiceImpl();
        }
        return instance;
    }

    @Override
    public Purchase createPendingPurchase(Cart cart,
            Address address, User requester,
            @Nullable User defaultApproverOrNull) {
        Objects.requireNonNull(cart, "Cart must not be null");
        Objects.requireNonNull(address, "Address must not be null");
        Objects.requireNonNull(requester, "Requester must not be null");

        if (cart.isEmpty()) {
            throw new IllegalArgumentException("Cart cannot be empty");
        }

        logger.info("Creating pending purchase for requester: '{}'",
                requester.getName());

        // Create the purchase entity
        Purchase purchase = new Purchase();
        purchase.setRequester(requester);
        purchase.setStatus(PurchaseStatus.PENDING);
        purchase.setCreatedAt(Instant.now());

        // Snapshot the delivery address
        Address addressSnapshot = new Address(address.getStreet(),
                address.getPostalCode(), address.getCity(),
                address.getCountry());
        purchase.setDeliveryAddress(addressSnapshot);

        // Determine approver
        User approver = defaultApproverOrNull;
        if (approver == null) {
            // Try to find default supervisor
            approver = purchaseDao.findSupervisorForEmployee(requester);
        }
        purchase.setApprover(approver);

        // Create purchase lines from cart
        for (Map.Entry<Product, Integer> entry : cart.getItems().entrySet()) {
            Product product = entry.getKey();
            Integer quantity = entry.getValue();

            // Snapshot the current price
            PurchaseLine line = new PurchaseLine();
            line.setPurchase(purchase);
            line.setProduct(product);
            line.setQuantity(quantity);
            line.setUnitPrice(product.getPrice());

            purchase.addLine(line);
        }

        // Persist the purchase (with cascading to lines)
        Purchase savedPurchase = purchaseDao.updatePurchase(purchase);

        logger.info("Created pending purchase with ID: {} for requester: '{}'",
                savedPurchase.getId(), requester.getName());

        return savedPurchase;
    }

    @Override
    public List<@NonNull Purchase> findMyPurchases(User requester,
            int offset, int limit) {
        Objects.requireNonNull(requester, "Requester must not be null");
        return purchaseDao.findByRequester(requester, offset, limit);
    }

    @Override
    public long countMyPurchases(User requester) {
        Objects.requireNonNull(requester, "Requester must not be null");
        return purchaseDao.countByRequester(requester);
    }

    @Override
    public List<@NonNull Purchase> findAll(int offset, int limit) {
        return purchaseDao.findAll(offset, limit);
    }

    @Override
    public long countAll() {
        return purchaseDao.countAll();
    }

    @Override
    public List<@NonNull Purchase> findPendingForApprover(
            User approver, int offset, int limit) {
        Objects.requireNonNull(approver, "Approver must not be null");
        return purchaseDao.findByApproverAndStatus(approver,
                PurchaseStatus.PENDING, offset, limit);
    }

    @Override
    public long countPendingForApprover(User approver) {
        Objects.requireNonNull(approver, "Approver must not be null");
        return purchaseDao.countByApproverAndStatus(approver,
                PurchaseStatus.PENDING);
    }

    @Override
    public List<@NonNull Purchase> fetchPurchases(PurchaseHistoryMode mode,
            int offset, int limit, User currentUser) {
        Objects.requireNonNull(mode, "Mode must not be null");
        Objects.requireNonNull(currentUser, "Current user must not be null");
        return switch (mode) {
        case MY_PURCHASES ->
            purchaseDao.findByRequester(currentUser, offset, limit);
        case ALL -> purchaseDao.findAll(offset, limit);
        case PENDING_APPROVALS -> purchaseDao.findByApproverAndStatus(
                currentUser, PurchaseStatus.PENDING, offset, limit);
        };
    }

    @Override
    public long countPurchases(PurchaseHistoryMode mode, User currentUser) {
        Objects.requireNonNull(mode, "Mode must not be null");
        Objects.requireNonNull(currentUser, "Current user must not be null");
        return switch (mode) {
        case MY_PURCHASES -> purchaseDao.countByRequester(currentUser);
        case ALL -> purchaseDao.countAll();
        case PENDING_APPROVALS -> purchaseDao
                .countByApproverAndStatus(currentUser, PurchaseStatus.PENDING);
        };
    }

    @Override
    public List<@NonNull Purchase> findRecentlyDecidedPurchases(
            User requester, Instant since) {
        Objects.requireNonNull(requester, "Requester must not be null");
        Objects.requireNonNull(since, "Since timestamp must not be null");
        return purchaseDao.findRecentlyDecidedByRequester(requester, since);
    }

    @Override
    public Purchase approve(Integer purchaseId, User currentUser,
            @Nullable String decisionCommentOrNull) {
        Objects.requireNonNull(purchaseId, "Purchase ID must not be null");
        Objects.requireNonNull(currentUser, "Current user must not be null");
        logger.info("Approving purchase: ({}) by user: '{}'", purchaseId,
                currentUser.getName());
        return purchaseDao.approvePurchase(purchaseId, currentUser,
                decisionCommentOrNull);
    }

    @Override
    public Purchase reject(Integer purchaseId, User currentUser,
            String reason) {
        Objects.requireNonNull(purchaseId, "Purchase ID must not be null");
        Objects.requireNonNull(currentUser, "Current user must not be null");
        Objects.requireNonNull(reason, "Reason must not be null");
        logger.info("Rejecting purchase: ({}) by user: '{}'", purchaseId,
                currentUser.getName());
        return purchaseDao.rejectPurchase(purchaseId, currentUser, reason);
    }

    @SuppressWarnings("null")
    private Logger logger = LoggerFactory.getLogger(this.getClass());
}
