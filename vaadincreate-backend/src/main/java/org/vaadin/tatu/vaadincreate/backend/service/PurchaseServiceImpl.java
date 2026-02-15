package org.vaadin.tatu.vaadincreate.backend.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.tatu.vaadincreate.backend.PurchaseService;
import org.vaadin.tatu.vaadincreate.backend.dao.PurchaseDao;
import org.vaadin.tatu.vaadincreate.backend.data.Address;
import org.vaadin.tatu.vaadincreate.backend.data.Cart;
import org.vaadin.tatu.vaadincreate.backend.data.Product;
import org.vaadin.tatu.vaadincreate.backend.data.Purchase;
import org.vaadin.tatu.vaadincreate.backend.data.PurchaseLine;
import org.vaadin.tatu.vaadincreate.backend.data.PurchaseStatus;
import org.vaadin.tatu.vaadincreate.backend.data.User;

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

    @SuppressWarnings("null")
    private Logger logger = LoggerFactory.getLogger(this.getClass());
}
