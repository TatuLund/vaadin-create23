package org.vaadin.tatu.vaadincreate.storefront;

import java.io.Serializable;
import java.util.Collection;
import java.util.stream.Collectors;

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
                .collect(Collectors.toList());
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
        return getUserService().getAllUsers().stream().filter(
                u -> u.getRole() == Role.USER || u.getRole() == Role.ADMIN)
                .collect(Collectors.toList());
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
