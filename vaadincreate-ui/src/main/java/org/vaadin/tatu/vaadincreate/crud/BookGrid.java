package org.vaadin.tatu.vaadincreate.crud;

import java.text.DecimalFormat;
import java.util.Comparator;
import java.util.stream.Collectors;

import org.vaadin.tatu.vaadincreate.VaadinCreateTheme;
import org.vaadin.tatu.vaadincreate.backend.data.Category;
import org.vaadin.tatu.vaadincreate.backend.data.Product;

import com.vaadin.icons.VaadinIcons;
import com.vaadin.shared.Registration;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Label;
import com.vaadin.ui.renderers.HtmlRenderer;
import com.vaadin.ui.renderers.NumberRenderer;

/**
 * Grid of products, handling the visual presentation and filtering of a set of
 * items. This version uses an in-memory data source that is suitable for small
 * data sets.
 */
@SuppressWarnings("serial")
public class BookGrid extends Grid<Product> {

    private Registration resizeReg;
    private Label availabilityCaption;

    private Product editedProduct;
    private int edited;

    public BookGrid() {
        setId("book-grid");
        setSizeFull();

        setStyleGenerator(book -> book.getId() == edited
                ? VaadinCreateTheme.BOOKVIEW_GRID_EDITED
                : "");

        addColumn(Product::getId, new NumberRenderer()).setCaption("Id");
        addColumn(Product::getProductName).setId("name")
                .setCaption("Product Name");

        // Format and add " €" to price
        var decimalFormat = new DecimalFormat();
        decimalFormat.setMaximumFractionDigits(2);
        decimalFormat.setMinimumFractionDigits(2);
        addColumn(product -> decimalFormat.format(product.getPrice()) + " €")
                .setCaption("Price").setComparator((p1, p2) -> {
                    return p1.getPrice().compareTo(p2.getPrice());
                }).setStyleGenerator(product -> "align-right").setId("price");

        // Add an traffic light icon in front of availability
        addColumn(this::htmlFormatAvailability, new HtmlRenderer())
                .setComparator((p1, p2) -> {
                    return p1.getAvailability().toString()
                            .compareTo(p2.getAvailability().toString());
                }).setId("availability");
        availabilityCaption = new Label("Availability");
        availabilityCaption
                .addStyleName(VaadinCreateTheme.BOOKVIEW_AVAILABILITYLABEL);

        // Show empty stock as "-"
        addColumn(product -> {
            if (product.getStockCount() == 0) {
                return "-";
            }
            return Integer.toString(product.getStockCount());
        }).setCaption("Stock Count").setComparator((p1, p2) -> {
            return Integer.compare(p1.getStockCount(), p2.getStockCount());
        }).setStyleGenerator(
                product -> VaadinCreateTheme.BOOKVIEW_GRID_ALIGNRIGHT)
                .setId("stock");

        // Show all categories the product is in, separated by commas
        addColumn(this::formatCategories).setCaption("Category")
                .setSortable(false);
    }

    public Product getSelectedRow() {
        return asSingleSelect().getValue();
    }

    public void refresh(Product product) {
        getDataCommunicator().refresh(product);
    }

    private String htmlFormatAvailability(Product product) {
        var availability = product.getAvailability();
        var text = availability.toString();

        var color = "";
        switch (availability) {
        case AVAILABLE:
            color = "#2dd085";
            break;
        case COMING:
            color = "#ffc66e";
            break;
        case DISCONTINUED:
            color = "#f54993";
            break;
        default:
            break;
        }

        var iconCode = "<span class=\"v-icon\" style=\"font-family: "
                + VaadinIcons.CIRCLE.getFontFamily() + ";color:" + color
                + "\">&#x"
                + Integer.toHexString(VaadinIcons.CIRCLE.getCodepoint())
                + ";</span>";

        return iconCode + "<span class=\""
                + VaadinCreateTheme.BOOKVIEW_AVAILABILITYLABEL + "\"> " + text
                + "</span>";
    }

    private String formatCategories(Product product) {
        if (product.getCategory() == null || product.getCategory().isEmpty()) {
            return "";
        }
        return product.getCategory().stream()
                .sorted(Comparator.comparing(Category::getId))
                .map(Category::getName).collect(Collectors.joining(", "));
    }

    @Override
    public void attach() {
        super.attach();
        getHeader().getDefaultRow().getCell("availability")
                .setComponent(availabilityCaption);
        var width = getUI().getPage().getBrowserWindowWidth();
        adjustColumns(width);
        resizeReg = getUI().getPage().addBrowserWindowResizeListener(e -> {
            adjustColumns(e.getWidth());
        });
    }

    private void adjustColumns(int width) {
        getColumns().forEach(c -> c.setHidden(true));
        if (width < 650) {
            getColumn("name").setHidden(false).setWidth(300);
            getColumn("price").setHidden(false);
        } else if (width < 920) {
            getColumn("name").setHidden(false).setWidthUndefined();
            getColumn("price").setHidden(false);
            getColumn("availability").setHidden(false);
        } else if (width < 1300) {
            getColumn("name").setHidden(false).setWidthUndefined();
            getColumn("price").setHidden(false);
            getColumn("stock").setHidden(false);
            getColumn("availability").setHidden(false);
        } else {
            getColumns().forEach(c -> c.setHidden(false));
        }
        recalculateColumnWidths();
    }

    @Override
    public void detach() {
        resizeReg.remove();
        super.detach();
    }

    public void setEdited(Product product) {
        if (edited != -1) {
            edited = -1;
            if (editedProduct != null) {
                getDataProvider().refreshItem(editedProduct);
            }
        }
        edited = product != null ? product.getId() : -1;
        editedProduct = product;
    }
}
