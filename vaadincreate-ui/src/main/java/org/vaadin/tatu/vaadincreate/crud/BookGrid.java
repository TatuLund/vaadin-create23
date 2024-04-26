package org.vaadin.tatu.vaadincreate.crud;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Comparator;
import java.util.stream.Collectors;

import org.vaadin.tatu.vaadincreate.VaadinCreateTheme;
import org.vaadin.tatu.vaadincreate.backend.data.Availability;
import org.vaadin.tatu.vaadincreate.backend.data.Category;
import org.vaadin.tatu.vaadincreate.backend.data.Product;
import org.vaadin.tatu.vaadincreate.i18n.HasI18N;
import org.vaadin.tatu.vaadincreate.util.Utils;

import com.vaadin.data.ValueContext;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.shared.Registration;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextField;
import com.vaadin.ui.renderers.HtmlRenderer;
import com.vaadin.ui.renderers.NumberRenderer;

/**
 * Grid of products, handling the visual presentation and filtering of a set of
 * items. This version uses an in-memory data source that is suitable for small
 * data sets.
 */
@SuppressWarnings("serial")
public class BookGrid extends Grid<Product> implements HasI18N {

    private static final String CATEGORIES = "categories";
    private static final String IN_STOCK = "in-stock";
    private static final String AVAILABILITY = "availability";
    private static final String PRICE = "price";
    private static final String PRODUCT_NAME = "product-name";
    private static final String CANNOT_CONVERT = "cannot-convert";

    private Registration resizeReg;
    private Label availabilityCaption;

    private Product editedProduct;
    private int edited;
    private DecimalFormat decimalFormat;

    public BookGrid() {
        setId("book-grid");
        setSizeFull();

        // Set highlight color to last edited row with style generator.
        setStyleGenerator(book -> book.getId() == edited
                ? VaadinCreateTheme.BOOKVIEW_GRID_EDITED
                : "");

        addColumn(Product::getId, new NumberRenderer()).setCaption("Id")
                .setResizable(false);
        addColumn(Product::getProductName).setId("name")
                .setCaption(getTranslation(PRODUCT_NAME)).setResizable(false)
                .setComparator((p1, p2) -> p1.getProductName()
                        .compareToIgnoreCase(p2.getProductName()));

        // Format and add " €" to price
        addColumn(product -> decimalFormat.format(product.getPrice()) + " €")
                .setCaption(getTranslation(PRICE)).setResizable(true)
                .setComparator((p1, p2) -> {
                    return p1.getPrice().compareTo(p2.getPrice());
                }).setStyleGenerator(product -> "align-right").setId("price");

        // Add an traffic light icon in front of availability
        addColumn(this::htmlFormatAvailability, new HtmlRenderer())
                .setResizable(false).setComparator((p1, p2) -> {
                    return p1.getAvailability().toString()
                            .compareTo(p2.getAvailability().toString());
                }).setId("availability");
        availabilityCaption = new Label(getTranslation(AVAILABILITY));
        availabilityCaption
                .addStyleName(VaadinCreateTheme.BOOKVIEW_AVAILABILITYLABEL);

        // Show empty stock as "-"
        addColumn(product -> {
            if (product.getStockCount() == 0) {
                return "-";
            }
            return Integer.toString(product.getStockCount());
        }).setCaption(getTranslation(IN_STOCK)).setResizable(false)
                .setComparator((p1, p2) -> {
                    return Integer.compare(p1.getStockCount(),
                            p2.getStockCount());
                })
                .setStyleGenerator(
                        product -> VaadinCreateTheme.BOOKVIEW_GRID_ALIGNRIGHT)
                .setId("stock");

        // Show all categories the product is in, separated by commas
        addColumn(this::formatCategories).setCaption(getTranslation(CATEGORIES))
                .setResizable(false).setSortable(false);
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

        var iconCode = createAvailabilityIcon(availability);

        return iconCode + "<span class=\""
                + VaadinCreateTheme.BOOKVIEW_AVAILABILITYLABEL + "\"> " + text
                + "</span>";
    }

    private static String createAvailabilityIcon(Availability availability) {
        var color = "";
        switch (availability) {
        case AVAILABLE:
            color = VaadinCreateTheme.COLOR_AVAILABLE;
            break;
        case COMING:
            color = VaadinCreateTheme.COLOR_COMING;
            break;
        case DISCONTINUED:
            color = VaadinCreateTheme.COLOR_DISCONTINUED;
            break;
        default:
            break;
        }

        var iconCode = "<span class=\"v-icon\" style=\"font-family: "
                + VaadinIcons.CIRCLE.getFontFamily() + ";color:" + color
                + "\">&#x"
                + Integer.toHexString(VaadinIcons.CIRCLE.getCodepoint())
                + ";</span>";
        return iconCode;
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
        // Replace caption with Label component so that it is possible to
        // control its visibility with CSS in the theme.
        getHeader().getDefaultRow().getCell("availability")
                .setComponent(availabilityCaption);
        // Get initial width and set visible columns based on that.
        var width = getUI().getPage().getBrowserWindowWidth();
        adjustColumns(width);
        // Add resize listener to update visible columns based on width
        resizeReg = getUI().getPage().addBrowserWindowResizeListener(e -> {
            adjustColumns(e.getWidth());
        });

        // Ensure that we use the same format as the Converter
        decimalFormat = (DecimalFormat) NumberFormat
                .getNumberInstance(getUI().getLocale());
        decimalFormat.setMaximumFractionDigits(2);
        decimalFormat.setMinimumFractionDigits(2);
    }

    private void adjustColumns(int width) {
        setDescriptionGenerator(null);
        getColumns().forEach(c -> c.setHidden(true));
        if (width < 650) {
            getColumn("name").setHidden(false).setWidth(300);
            getColumn("price").setHidden(false);
            setDescriptionGenerator(book -> createTooltip(book),
                    ContentMode.HTML);
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

    private String createTooltip(Product book) {
        // This is not actually a tooltip, but on hover popup. Vaadin 8 allows
        // to use HTML in tooltips. which makes it possible to use them like
        // this. When migrating to newer generations of Vaadin, this kind of
        // Tooltips need to be refactored to use for example Popup component.
        var converter = new EuroConverter(getTranslation(CANNOT_CONVERT));
        StringBuilder unsanitized = new StringBuilder();
        unsanitized
                .append("<div><span class='bookview-grid-descriptioncaption'>")
                .append(getTranslation(PRODUCT_NAME)).append(":</span> <b>")
                .append(book.getProductName())
                .append("</b><br><span class='bookview-grid-descriptioncaption'>")
                .append(getTranslation(PRICE)).append("</span> ")
                .append(converter.convertToPresentation(book.getPrice(),
                        createValueContext()))
                .append("<br><span class='bookview-grid-descriptioncaption'>")
                .append(getTranslation(AVAILABILITY)).append(":</span> ")
                .append(createAvailabilityIcon(book.getAvailability()))
                .append("<br><span class='bookview-grid-descriptioncaption'>")
                .append(getTranslation(IN_STOCK)).append(":</span> ")
                .append(book.getStockCount())
                .append("<br><span class='bookview-grid-descriptioncaption'>")
                .append(getTranslation(CATEGORIES)).append(":</span> ")
                .append(formatCategories(book)).append("</div>");
        return Utils.sanitize(unsanitized.toString());
    }

    private static ValueContext createValueContext() {
        var field = new TextField();
        return new ValueContext(field, field);
    }

    @Override
    public void detach() {
        // It is necessary to remove resize listener upon detach to avoid
        // resource leakage.
        resizeReg.remove();
        super.detach();
    }

    public void setEdited(Product product) {
        if (edited != -1) {
            edited = -1;
            if (editedProduct != null) {
                // Apply style generator
                getDataProvider().refreshItem(editedProduct);
            }
        }
        edited = product != null ? product.getId() : -1;
        editedProduct = product;
    }
}
