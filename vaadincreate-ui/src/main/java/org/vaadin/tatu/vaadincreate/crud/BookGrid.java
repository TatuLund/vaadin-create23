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
import org.vaadin.tatu.vaadincreate.locking.LockedObjects;
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
@SuppressWarnings({ "serial", "java:S2160" })
public class BookGrid extends Grid<Product> implements HasI18N {

    // Column keys
    private static final String STOCK_ID = "stock";
    private static final String NAME_ID = "name";
    private static final String AVAILABILITY_ID = "availability";
    private static final String PRICE_ID = "price";

    // I18N keys
    private static final String CATEGORIES = "categories";
    private static final String IN_STOCK = "in-stock";
    private static final String AVAILABILITY = AVAILABILITY_ID;
    private static final String PRICE = PRICE_ID;
    private static final String PRODUCT_NAME = "product-name";
    private static final String CANNOT_CONVERT = "cannot-convert";
    private static final String EDITED_BY = "edited-by";

    private Registration resizeReg;
    private Label availabilityCaption;

    private Product editedProduct;
    private int edited;
    private DecimalFormat decimalFormat;

    /**
     * The BookGrid class represents a grid component that displays a list of
     * books. It provides various columns to display different properties of the
     * books, such as id, name, price, availability, stock count, and
     * categories. The grid also supports highlighting the last edited row and
     * showing a traffic light icon for availability.
     */
    public BookGrid() {
        setId("book-grid");
        setSizeFull();

        // Set highlight color to last edited row with style generator.
        setStyleGenerator(book -> {
            if (book.getId() == edited) {
                return VaadinCreateTheme.BOOKVIEW_GRID_EDITED;
            }
            if (getLockedBooks().isLocked(book) != null) {
                return VaadinCreateTheme.BOOKVIEW_GRID_LOCKED;
            }
            return "";
        });

        addColumn(Product::getId, new NumberRenderer()).setCaption("Id")
                .setResizable(false);
        addColumn(Product::getProductName).setId(NAME_ID)
                .setCaption(getTranslation(PRODUCT_NAME)).setResizable(false)
                .setComparator((p1, p2) -> p1.getProductName()
                        .compareToIgnoreCase(p2.getProductName()));

        // Format and add " €" to price
        var priceCol = addColumn(
                product -> decimalFormat.format(product.getPrice()) + " €")
                        .setCaption(getTranslation(PRICE)).setResizable(true)
                        .setComparator((p1, p2) -> {
                            return p1.getPrice().compareTo(p2.getPrice());
                        })
                        .setStyleGenerator(
                                product -> VaadinCreateTheme.BOOKVIEW_GRID_ALIGNRIGHT)
                        .setId(PRICE_ID);

        // Add an traffic light icon in front of availability
        addColumn(this::htmlFormatAvailability, new HtmlRenderer())
                .setResizable(false).setComparator((p1, p2) -> {
                    return p1.getAvailability().toString()
                            .compareTo(p2.getAvailability().toString());
                }).setId(AVAILABILITY_ID);
        availabilityCaption = new Label(getTranslation(AVAILABILITY));
        availabilityCaption
                .addStyleName(VaadinCreateTheme.BOOKVIEW_AVAILABILITYLABEL);

        // Show empty stock as "-"
        var countCol = addColumn(product -> {
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
                .setId(STOCK_ID);

        // Show all categories the product is in, separated by commas
        addColumn(this::formatCategories).setCaption(getTranslation(CATEGORIES))
                .setResizable(false).setSortable(false);

        getHeaderRow(0).getCell(priceCol)
                .setStyleName(VaadinCreateTheme.BOOKVIEW_GRID_ALIGNRIGHT);
        getHeaderRow(0).getCell(countCol)
                .setStyleName(VaadinCreateTheme.BOOKVIEW_GRID_ALIGNRIGHT);
    }

    public Product getSelectedRow() {
        return asSingleSelect().getValue();
    }

    private String htmlFormatAvailability(Product product) {
        var availability = product.getAvailability();
        var text = availability.toString();

        var iconCode = createAvailabilityIcon(availability);

        return iconCode + "<span class=\""
                + VaadinCreateTheme.BOOKVIEW_AVAILABILITYLABEL + "\"> " + text
                + "</span>";
    }

    // Helper method to create an icon for availability
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

        return "<span class=\"v-icon\" style=\"font-family: "
                + VaadinIcons.CIRCLE.getFontFamily() + ";color:" + color
                + "\">&#x"
                + Integer.toHexString(VaadinIcons.CIRCLE.getCodepoint())
                + ";</span>";
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
        getHeader().getDefaultRow().getCell(AVAILABILITY_ID)
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

    /**
     * Adjusts the visibility and width of columns in the book grid based on the
     * specified width.
     *
     * @param width
     *            the width of the grid
     */
    private void adjustColumns(int width) {
        setDescriptionGenerator(book -> {
            var user = getLockedBooks().isLocked(book);
            if (user != null) {
                return getTranslation(EDITED_BY, user.getName());
            }
            return null;
        });
        getColumns().forEach(c -> c.setHidden(true));
        if (width < 650) {
            getColumn(NAME_ID).setHidden(false).setWidth(300);
            getColumn(PRICE_ID).setHidden(false);
            setDescriptionGenerator(this::createTooltip, ContentMode.HTML);
        } else if (width < 920) {
            getColumn(NAME_ID).setHidden(false).setWidthUndefined();
            getColumn(PRICE_ID).setHidden(false);
            getColumn(AVAILABILITY_ID).setHidden(false);
        } else if (width < 1300) {
            getColumn(NAME_ID).setHidden(false).setWidthUndefined();
            getColumn(PRICE_ID).setHidden(false);
            getColumn(STOCK_ID).setHidden(false);
            getColumn(AVAILABILITY_ID).setHidden(false);
        } else {
            getColumns().forEach(c -> c.setHidden(false));
        }
    }

    private String createTooltip(Product book) {
        // This is not actually a tooltip, but on hover popup. Vaadin 8 allows
        // to use HTML in tooltips. which makes it possible to use them like
        // this. When migrating to newer generations of Vaadin, this kind of
        // Tooltips need to be refactored to use for example Popup component.
        var user = getLockedBooks().isLocked(book);
        if (user != null) {
            return getTranslation(EDITED_BY, user.getName());
        }
        var converter = new EuroConverter(getTranslation(CANNOT_CONVERT));
        StringBuilder unsanitized = new StringBuilder();
        unsanitized.append("<div>")
                .append(getDescriptionCaptionSpan(getTranslation(PRODUCT_NAME)))
                .append(" <b>").append(book.getProductName()).append("</b><br>")
                .append(getDescriptionCaptionSpan(getTranslation(PRICE)))
                .append(converter.convertToPresentation(book.getPrice(),
                        createValueContext()))
                .append("<br>")
                .append(getDescriptionCaptionSpan(getTranslation(AVAILABILITY)))
                .append(createAvailabilityIcon(book.getAvailability()))
                .append("<br>")
                .append(getDescriptionCaptionSpan(getTranslation(IN_STOCK)))
                .append(book.getStockCount()).append("<br>")
                .append(getDescriptionCaptionSpan(getTranslation(CATEGORIES)))
                .append(formatCategories(book)).append("</div>");
        return Utils.sanitize(unsanitized.toString());
    }

    // Helper method to create a span with a caption
    private static String getDescriptionCaptionSpan(String caption) {
        return "<span class='"
                + VaadinCreateTheme.BOOKVIEW_GRID_DESCRIPTIONCAPTION + "'>"
                + caption + ":</span> ";
    }

    // Helper method to create a ValueContext for the EuroConverter
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

    /**
     * Sets the product to be the last edited product in the grid. It will be
     * shown with a different style.
     *
     * @param product
     *            the product to be highlighted as last edited
     */
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

    private LockedObjects getLockedBooks() {
        return LockedObjects.get();
    }
}
