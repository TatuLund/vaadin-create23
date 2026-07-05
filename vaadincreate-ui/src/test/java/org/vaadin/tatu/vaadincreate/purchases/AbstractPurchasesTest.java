package org.vaadin.tatu.vaadincreate.purchases;

import java.time.LocalDate;
import java.time.ZoneId;

import org.junit.After;
import org.junit.Before;
import org.vaadin.tatu.vaadincreate.AbstractUITest;
import org.vaadin.tatu.vaadincreate.VaadinCreateUI;
import org.vaadin.tatu.vaadincreate.backend.ProductDataService;
import org.vaadin.tatu.vaadincreate.backend.PurchaseHistoryMode;
import org.vaadin.tatu.vaadincreate.backend.PurchaseService;
import org.vaadin.tatu.vaadincreate.backend.data.Purchase;
import org.vaadin.tatu.vaadincreate.backend.data.PurchaseStatus;
import org.vaadin.tatu.vaadincreate.util.Utils;

import com.vaadin.server.ServiceException;
import com.vaadin.ui.Button;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.Grid;

public abstract class AbstractPurchasesTest extends AbstractUITest {

    VaadinCreateUI ui;
    PurchasesView view;

    @Before
    public void setup() throws ServiceException {
        ui = new VaadinCreateUI();
        mockVaadin(ui);
        login();
    }

    @After
    public void cleanUp() {
        logout();
        tearDown();
    }

    Button getApproveButton(Grid<Purchase> approvalsGrid) {
        var approveActionLayout = (CssLayout) test(approvalsGrid).cell(9,
                0);
        return $(approveActionLayout, Button.class).first();
    }

    public static void restoreProductStockLevels(Purchase pendingPurchase) {
        // Restore product stock levels for the approved purchase so that the
        // test has no statistics side effect
        var purchase = PurchaseService.get()
                .fetchPurchaseById(pendingPurchase.getId());
        if (purchase.getStatus() == PurchaseStatus.COMPLETED) {
            purchase.getLines().forEach(line -> {
                var product = ProductDataService.get()
                        .getProductById(line.getProduct().getId());
                var quantity = line.getQuantity();
                product.setStockCount(product.getStockCount() + quantity);
                ProductDataService.get().updateProduct(product);
            });
        }
    }

    Button getRejectButton(Grid<Purchase> approvalsGrid) {
        var rejectActionLayout = (CssLayout) test(approvalsGrid).cell(9,
                0);
        return $(rejectActionLayout, Button.class).stream().skip(1).findFirst()
                .orElseThrow();
    }

    void switchToUser(String name, String password)
            throws ServiceException {
        logout();
        tearDown();
        ui = new VaadinCreateUI();
        mockVaadin(ui);
        login(name, password);
    }

    LocalDate lastPurchaseDate() {
        var service = PurchaseService.get();
        var user = Utils.getCurrentUserOrThrow();
        var purchases = service.fetchPurchases(PurchaseHistoryMode.ALL, 0, 1,
                user);
        var instant = purchases.get(0).getCreatedAt();
        return instant.atZone(ZoneId.systemDefault()).toLocalDate();
    }

    static void swapApprovalsPresenter(
            PurchasesApprovalsView approvalsView,
            PurchasesApprovalsPresenter replacement) throws Exception {
        var field = PurchasesApprovalsView.class.getDeclaredField("presenter");
        field.setAccessible(true);
        field.set(approvalsView, replacement);
    }
}
