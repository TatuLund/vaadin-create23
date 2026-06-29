package org.vaadin.tatu.vaadincreate.storefront;

import java.math.BigDecimal;

import org.vaadin.tatu.vaadincreate.VaadinCreateTheme;
import org.vaadin.tatu.vaadincreate.common.EuroConverter;
import org.vaadin.tatu.vaadincreate.common.EuroRenderer;
import org.vaadin.tatu.vaadincreate.common.NumberField;
import org.vaadin.tatu.vaadincreate.i18n.HasI18N;
import org.vaadin.tatu.vaadincreate.i18n.I18n;
import org.vaadin.tatu.vaadincreate.util.Utils;

import com.vaadin.data.provider.Query;
import com.vaadin.shared.data.sort.SortDirection;
import com.vaadin.ui.Grid;
import com.vaadin.ui.components.grid.FooterRow;
import com.vaadin.ui.components.grid.MultiSelectionModel;
import com.vaadin.ui.components.grid.MultiSelectionModel.SelectAllCheckBoxVisibility;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.JavaScript;
import com.vaadin.ui.Label;

@SuppressWarnings("java:S2160")
class ProductGrid extends Grid<ProductDto> implements HasI18N {

    private final FooterRow footerRow;

    @SuppressWarnings("null")
    ProductGrid() {
        super(ProductDto.class);
        setId("purchase-grid");
        setSizeFull();
        setSelectionMode(SelectionMode.MULTI);
        setAccessibleNavigation(true);

        // Configure columns
        removeAllColumns();
        addColumn(ProductDto::getProductName)
                .setCaption(getTranslation(I18n.PRODUCT_NAME))
                .setMaximumWidth(300)
                .setComparator((p1, p2) -> p1.getProductName()
                        .compareToIgnoreCase(p2.getProductName()));
        addColumn(ProductDto::getStockCount)
                .setCaption(getTranslation(I18n.IN_STOCK))
                .setComparator((p1, p2) -> Integer
                        .compare(p1.getStockCount(), p2.getStockCount()));
        addColumn(ProductDto::getPrice, new EuroRenderer())
                .setCaption(getTranslation(I18n.PRICE)).setComparator(
                        (p1, p2) -> p1.getPrice().compareTo(p2.getPrice()));

        // Add quantity column with NumberField
        addComponentColumn(dto -> {
            var layout = new HorizontalLayout();
            layout.setMargin(false);
            layout.setSpacing(false);
            layout.setWidth("100%");
            if (getSelectedItems().contains(dto)) {
                var numberField = new NumberField(null);
                numberField.setAriaLabel(
                        getTranslation(I18n.Storefront.QUANTITY));
                numberField.setValue(dto.getOrderQuantity());
                numberField.setWidth("100%");
                numberField.addValueChangeListener(e -> {
                    dto.setOrderQuantity(e.getValue());
                    updateFooter();
                    sort("quantity-column", SortDirection.DESCENDING);
                });
                numberField.setId("quantity-for-" + dto.getProductId());
                layout.addComponent(numberField);
            } else {
                layout.addComponent(new Label("-"));
            }
            return layout;
        }).setComparator((p1, p2) -> Integer.compare(p1.getOrderQuantity(),
                p2.getOrderQuantity()))
                .setCaption(getTranslation(I18n.Storefront.QUANTITY))
                .setId("quantity-column").setStyleGenerator(
                        product -> VaadinCreateTheme.STOREFRONTVIEW_WIZARD_QUANTITYCOLUMN);

        // Footer row for totals
        footerRow = appendFooterRow();
        footerRow.getCell(getColumns().get(0))
                .setText(getTranslation(I18n.Storefront.ORDER_SUMMARY));

        ((MultiSelectionModel<ProductDto>) this.getSelectionModel())
                .setSelectAllCheckBoxVisibility(
                        SelectAllCheckBoxVisibility.HIDDEN);

        // Update quantities when selection changes
        addSelectionListener(e -> {
            for (var dto : getDataProvider().fetch(new Query<>())
                    .toList()) {
                if (!e.getAllSelectedItems().contains(dto)) {
                    dto.setOrderQuantity(0);
                }
            }
            getDataProvider().refreshAll();
            updateFooter();
        });
    }

    void updateFooter() {
        var totalQuantity = 0;
        var totalPrice = BigDecimal.ZERO;

        for (var dto : getDataProvider().fetch(new Query<>()).toList()) {
            if (dto.getOrderQuantity() > 0) {
                totalQuantity += dto.getOrderQuantity();
                totalPrice = totalPrice.add(dto.getLineTotal());
            }
        }

        var euroConverter = new EuroConverter("");
        footerRow.getCell(getColumns().get(2))
                .setText(euroConverter.convertToPresentation(totalPrice,
                        Utils.createValueContext()));
        footerRow.getCell(getColumns().get(3))
                .setText(String.valueOf(totalQuantity));
    }

    @Override
    public void attach() {
        super.attach();
        patchQuantityColumnFocus();
    }

    private void patchQuantityColumnFocus() {
        // Grid re-renders (e.g., refreshAll()) regenerate cell DOM. This
        // patch is
        // therefore both (1) installed once and (2) re-applicable to newly
        // generated cells/inputs without stacking listeners.
        JavaScript.getCurrent().execute(
                """
                        (function() {
                            window.__vaadincreateQuantityPatch = window.__vaadincreateQuantityPatch || {};
                            var state = window.__vaadincreateQuantityPatch;

                            function isQuantityCellElement(el) {
                                return el && el.closest && el.closest('td.storefrontview-wizard-quantitycolumn');
                            }

                            function applyPatch() {
                                // Patch any newly created inputs in quantity cells.
                                document.querySelectorAll('td.storefrontview-wizard-quantitycolumn input').forEach(function(input) {
                                    if (input.__vaadincreateQuantityPatched) {
                                        return;
                                    }
                                    input.__vaadincreateQuantityPatched = true;
                                    input.addEventListener('keydown', function(e) {
                                        if (e.key === 'ArrowUp' || e.key === 'ArrowDown') {
                                            e.preventDefault();
                                        }
                                    }, true);
                                });
                            }

                            // Expose so Java can re-apply after refreshAll().
                            state.apply = applyPatch;

                            if (!state.installed) {
                                state.installed = true;

                                // When a quantity cell receives focus, forward focus to its input.
                                document.addEventListener('focusin', function(e) {
                                    var cell = isQuantityCellElement(e.target);
                                    if (!cell) {
                                        return;
                                    }
                                    var input = cell.querySelector('input');
                                    if (input && e.target !== input) {
                                        input.focus();
                                    }
                                }, true);

                                // Observe DOM changes so newly generated cells also get patched.
                                // (Still safe to call state.apply() from Java after refreshAll().)
                                state.observer = new MutationObserver(function() {
                                    applyPatch();
                                });
                                if (document.body) {
                                    state.observer.observe(document.body, { childList: true, subtree: true });
                                }
                            }

                            // Apply immediately for current DOM.
                            state.apply();
                        })();
                        """);
    }
}
