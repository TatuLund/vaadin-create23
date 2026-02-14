package org.vaadin.tatu.vaadincreate.storefront;

import java.io.Serializable;
import java.util.Collection;
import java.util.stream.Collectors;

import org.jspecify.annotations.NullMarked;
import org.vaadin.tatu.vaadincreate.VaadinCreateUI;
import org.vaadin.tatu.vaadincreate.backend.ProductDataService;

/**
 * Presenter for StorefrontView. Separates backend access from the view layer.
 */
@NullMarked
@SuppressWarnings("serial")
public class StorefrontPresenter implements Serializable {

    private final ProductDataService productService = VaadinCreateUI.get()
            .getProductService();

    /**
     * Gets all orderable products as DTOs.
     * 
     * @return Collection of ProductDto objects
     */
    public Collection<ProductDto> getOrderableProducts() {
        return productService.getOrderableProducts().stream()
                .map(p -> new ProductDto(p.getId(), p.getProductName(),
                        p.getStockCount(), p.getPrice()))
                .collect(Collectors.toList());
    }
}
