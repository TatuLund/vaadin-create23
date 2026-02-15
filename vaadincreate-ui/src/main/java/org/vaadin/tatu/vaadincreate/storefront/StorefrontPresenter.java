package org.vaadin.tatu.vaadincreate.storefront;

import java.io.Serializable;
import java.time.Instant;
import java.util.Collection;
import java.util.List;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.vaadin.tatu.vaadincreate.VaadinCreateUI;
import org.vaadin.tatu.vaadincreate.backend.ProductDataService;
import org.vaadin.tatu.vaadincreate.backend.PurchaseService;
import org.vaadin.tatu.vaadincreate.backend.UserService;
import org.vaadin.tatu.vaadincreate.backend.data.Address;
import org.vaadin.tatu.vaadincreate.backend.data.Cart;
import org.vaadin.tatu.vaadincreate.backend.data.Product;
import org.vaadin.tatu.vaadincreate.backend.data.Purchase;
import org.vaadin.tatu.vaadincreate.backend.data.User;
import org.vaadin.tatu.vaadincreate.backend.data.User.Role;

/**
 * Presenter for StorefrontView. Separates backend access from the view layer.
 */
@NullMarked
@SuppressWarnings("serial")
public class StorefrontPresenter implements Serializable {

    private transient ProductDataService productService;
    private transient UserService userService;
    private transient PurchaseService purchaseService;

    /**
     * Gets all orderable products as DTOs.
     * 
     * @return Collection of ProductDto objects
     */
    public Collection<ProductDto> getOrderableProducts() {
        return getProductService().getOrderableProducts().stream()
                .map(p -> new ProductDto(p.getId(), p.getProductName(),
                        p.getStockCount(), p.getPrice()))
                .toList();
    }

    /**
     * Gets a product by ID.
     * 
     * @param productId
     *            the product ID
     * @return the Product entity
     */
    @Nullable
    public Product getProductById(Integer productId) {
        return getProductService().getProductById(productId);
    }

    /**
     * Gets all users with USER or ADMIN roles (potential supervisors).
     * 
     * @return Collection of User objects
     */
    public Collection<User> getSupervisors() {
        // Fetch users with USER role and combine with ADMIN role users
        var users = getUserService().getUsersByRole(Role.USER);
        var admins = getUserService().getUsersByRole(Role.ADMIN);
        users.addAll(admins);
        return users;
    }

    /**
     * Creates a pending purchase.
     * 
     * @param cart
     *            the shopping cart
     * @param address
     *            the delivery address
     * @param requester
     *            the user requesting the purchase
     * @param supervisor
     *            the supervisor for approval
     * @return the created Purchase
     */
    public Purchase createPendingPurchase(Cart cart, Address address,
            User requester, User supervisor) {
        return getPurchaseService().createPendingPurchase(cart, address,
                requester, supervisor);
    }

    /**
     * Fetches purchases for the given requester with pagination.
     * 
     * @param requester
     *            the user who created the purchases
     * @param offset
     *            the starting offset for pagination
     * @param limit
     *            the maximum number of results
     * @return list of purchases
     */
    public List<@NonNull Purchase> fetchMyPurchases(User requester, int offset,
            int limit) {
        return getPurchaseService().findMyPurchases(requester, offset, limit);
    }

    /**
     * Counts purchases for the given requester.
     * 
     * @param requester
     *            the user who created the purchases
     * @return the count of purchases
     */
    public long countMyPurchases(User requester) {
        return getPurchaseService().countMyPurchases(requester);
    }

    /**
     * Finds purchases that have been decided (COMPLETED or REJECTED) since the
     * given timestamp.
     * 
     * @param requester
     *            the user who created the purchases
     * @param since
     *            the timestamp to filter from
     * @return list of decided purchases since the given time
     */
    public List<@NonNull Purchase> findRecentlyDecidedPurchases(User requester,
            Instant since) {
        return getPurchaseService().findRecentlyDecidedPurchases(requester,
                since);
    }

    /**
     * Updates the lastStatusCheck timestamp for the given user.
     * 
     * @param user
     *            the user to update
     * @param timestamp
     *            the timestamp to set
     */
    public void updateLastStatusCheck(User user, Instant timestamp) {
        user.setLastStatusCheck(timestamp);
        getUserService().updateUser(user);
    }

    private ProductDataService getProductService() {
        if (productService == null) {
            productService = VaadinCreateUI.get().getProductService();
        }
        return productService;
    }

    private UserService getUserService() {
        if (userService == null) {
            userService = VaadinCreateUI.get().getUserService();
        }
        return userService;
    }

    private PurchaseService getPurchaseService() {
        if (purchaseService == null) {
            purchaseService = PurchaseService.get();
        }
        return purchaseService;
    }
}
