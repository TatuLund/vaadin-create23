package org.vaadin.tatu.vaadincreate.crud;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.vaadin.tatu.vaadincreate.VaadinCreateTheme;
import org.vaadin.tatu.vaadincreate.backend.data.Category;
import org.vaadin.tatu.vaadincreate.backend.data.Product;
import org.vaadin.tatu.vaadincreate.i18n.HasI18N;
import org.vaadin.tatu.vaadincreate.i18n.I18n;
import org.vaadin.tatu.vaadincreate.locking.LockedObjects;
import org.vaadin.tatu.vaadincreate.util.Utils;

import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.server.SerializablePredicate;
import com.vaadin.shared.Registration;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.Grid;
import com.vaadin.ui.JavaScript;
import com.vaadin.ui.Label;
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

    private Registration resizeReg;
    private Label availabilityCaption;

    private Product editedProduct;
    private int edited;

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
        addStyleNames(VaadinCreateTheme.GRID_NO_CELL_FOCUS,
                VaadinCreateTheme.GRID_ROW_FOCUS);

        // Set highlight color to last edited row with style generator.
        setStyleGenerator(book -> {
            if (book.getId() != null && book.getId() == edited) {
                return VaadinCreateTheme.BOOKVIEW_GRID_EDITED;
            }
            if (getLockedBooks().isLocked(book) != null) {
                return VaadinCreateTheme.BOOKVIEW_GRID_LOCKED;
            }
            return "";
        });

        addColumn(product -> product.getId() != null ? product.getId() : -1,
                new NumberRenderer()).setCaption("Id").setResizable(false);
        addColumn(Product::getProductName).setId(NAME_ID)
                .setCaption(getTranslation(I18n.PRODUCT_NAME))
                .setResizable(false)
                .setComparator((p1, p2) -> p1.getProductName()
                        .compareToIgnoreCase(p2.getProductName()));

        // Format and add " €" to price
        var priceCol = addColumn(
                product -> String.format("%.2f €", product.getPrice()))
                        .setCaption(getTranslation(I18n.PRICE))
                        .setResizable(true)
                        .setComparator((p1, p2) -> p1.getPrice()
                                .compareTo(p2.getPrice()))
                        .setStyleGenerator(
                                product -> VaadinCreateTheme.BOOKVIEW_GRID_ALIGNRIGHT)
                        .setId(PRICE_ID);

        // Add an traffic light icon in front of availability
        addColumn(this::htmlFormatAvailability, new HtmlRenderer())
                .setResizable(false)
                .setComparator((p1, p2) -> p1.getAvailability().toString()
                        .compareTo(p2.getAvailability().toString()))
                .setId(AVAILABILITY_ID);
        availabilityCaption = new Label(getTranslation(I18n.AVAILABILITY));
        availabilityCaption
                .addStyleName(VaadinCreateTheme.BOOKVIEW_AVAILABILITYLABEL);

        // Show empty stock as "-"
        var countCol = addColumn(product -> {
            if (product.getStockCount() == 0) {
                return "-";
            }
            return Integer.toString(product.getStockCount());
        }).setCaption(getTranslation(I18n.IN_STOCK)).setResizable(false)
                .setComparator((p1, p2) -> Integer.compare(p1.getStockCount(),
                        p2.getStockCount()))
                .setStyleGenerator(
                        product -> VaadinCreateTheme.BOOKVIEW_GRID_ALIGNRIGHT)
                .setId(STOCK_ID);

        // Show all categories the product is in, separated by commas
        addColumn(this::formatCategories)
                .setCaption(getTranslation(I18n.CATEGORIES)).setResizable(false)
                .setSortable(false);

        getHeaderRow(0).getCell(priceCol)
                .setStyleName(VaadinCreateTheme.BOOKVIEW_GRID_ALIGNRIGHT);
        getHeaderRow(0).getCell(countCol)
                .setStyleName(VaadinCreateTheme.BOOKVIEW_GRID_ALIGNRIGHT);
    }

    /**
     * Checks if the data provider associated with the data communicator has no
     * items.
     *
     * @return {@code true} if the data provider size is zero, {@code false}
     *         otherwise.
     */
    public boolean isEmpty() {
        return getDataCommunicator().getDataProviderSize() == 0;
    }

    /**
     * Finds a product by its ID from the data provider.
     *
     * @param id
     *            the ID of the product to find
     * @return the product with the specified ID, or {@code null} if no such
     *         product is found
     */
    @SuppressWarnings("unchecked")
    public Product findProductById(Integer id) {
        return ((ListDataProvider<Product>) getDataProvider()).getItems()
                .stream().filter(book -> book.getId().equals(id)).findFirst()
                .orElse(null);
    }

    /**
     * Adds a new product to the data provider and refreshes the grid.
     *
     * @param book
     *            the product to be added
     */
    @SuppressWarnings("unchecked")
    public void addProduct(Product book) {
        ((ListDataProvider<Product>) getDataProvider()).getItems().add(book);
        getDataProvider().refreshAll();
    }

    /**
     * Removes the specified product from the data provider and refreshes the
     * grid.
     *
     * @param book
     *            the product to be removed from the data provider
     */
    @SuppressWarnings("unchecked")
    public void removeProduct(Product book) {
        ((ListDataProvider<Product>) getDataProvider()).getItems().remove(book);
        getDataProvider().refreshAll();
    }

    /**
     * Refreshes the data provider to update the specified book item.
     *
     * @param book
     *            the book item to be refreshed
     */
    public void refresh(Product book) {
        getDataProvider().refreshItem(book);
    }

    /**
     * Replaces an existing product in the data provider with an updated
     * product.
     *
     * @param product
     *            the original product to be replaced
     * @param updatedProduct
     *            the new product to replace the original product
     * @throws ClassCastException
     *             if the data provider is not of type ListDataProvider<Product>
     * @throws IndexOutOfBoundsException
     *             if the original product is not found in the list
     */
    @SuppressWarnings("unchecked")
    public void replaceProduct(Product product, Product updatedProduct) {
        var list = ((List<Product>) ((ListDataProvider<Product>) getDataProvider())
                .getItems());
        list.set(list.indexOf(product), updatedProduct);
        refresh(updatedProduct);
    }

    /**
     * Checks if the current data provider is an instance of ListDataProvider.
     *
     * @return true if the data provider is an instance of ListDataProvider,
     *         false otherwise.
     */
    public boolean hasDataProdiver() {
        return getDataProvider() instanceof ListDataProvider;
    }

    /**
     * Sets a filter for the data provider of the grid.
     * 
     * @param filter
     *            the filter to be applied to the data provider which determines
     *            which items are displayed in the grid. The filter is a
     *            {@link SerializablePredicate} that takes a {@link Product} as
     *            input and returns a boolean indicating whether the product
     *            should be included.
     * 
     * @throws ClassCastException
     *             if the data provider is not an instance of
     *             {@link ListDataProvider}.
     */
    @SuppressWarnings("unchecked")
    public void setFilter(SerializablePredicate<Product> filter) {
        if (getDataProvider() instanceof ListDataProvider) {
            ((ListDataProvider<Product>) getDataProvider()).setFilter(filter);
        }
    }

    @Override
    public void setReadOnly(boolean readOnly) {
        super.setReadOnly(readOnly);
        if (!readOnly) {
            addStyleNames(VaadinCreateTheme.GRID_NO_CELL_FOCUS,
                    VaadinCreateTheme.GRID_ROW_FOCUS);
        } else {
            addStyleName(VaadinCreateTheme.GRID_NO_CELL_FOCUS);
            removeStyleName(VaadinCreateTheme.GRID_ROW_FOCUS);
        }
    }

    public Product getSelectedRow() {
        return asSingleSelect().getValue();
    }

    private String htmlFormatAvailability(Product product) {
        assert product != null : "Product must not be null";

        var availability = product.getAvailability();
        var text = availability.toString();

        var iconCode = Utils.createAvailabilityIcon(availability);

        return String.format("%s<span class=\"%s\"> %s</span>", iconCode,
                VaadinCreateTheme.BOOKVIEW_AVAILABILITYLABEL, text);
    }

    private String formatCategories(Product product) {
        assert product != null : "Product must not be null";

        if (product.getCategory().isEmpty()) {
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
        resizeReg = getUI().getPage().addBrowserWindowResizeListener(
                e -> adjustColumns(e.getWidth()));

        // Ensure that we use the same format as the Converter
        var decimalFormat = (DecimalFormat) NumberFormat
                .getNumberInstance(getUI().getLocale());
        decimalFormat.setMaximumFractionDigits(2);
        decimalFormat.setMinimumFractionDigits(2);
        // Improve Grid browsing experience for screen reader users
        JavaScript.eval("""
                setTimeout(() => {
                    const body = document.querySelector('tbody.v-grid-body');
                    Array.from(body.getElementsByTagName('tr')).forEach(el => {
                        el.setAttribute('aria-live', 'polite');
                    });
                }, 1000);
                """);
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
                return getTranslation(I18n.Grid.EDITED_BY, user.getName());
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

        if (width > 650 && width < 800) {
            getColumn(AVAILABILITY_ID).setDescriptionGenerator(
                    book -> book.getAvailability().toString());
        } else {
            getColumn(AVAILABILITY_ID).setDescriptionGenerator(null);
        }
    }

    private String createTooltip(Product book) {
        assert book != null : "Book must not be null";

        // This is not actually a tooltip, but on hover popup. Vaadin 8 allows
        // to use HTML in tooltips. which makes it possible to use them like
        // this. When migrating to newer generations of Vaadin, this kind of
        // Tooltips need to be refactored to use for example Popup component.
        var user = getLockedBooks().isLocked(book);
        if (user != null) {
            return getTranslation(I18n.Grid.EDITED_BY, user.getName());
        }
        var converter = new EuroConverter(
                getTranslation(I18n.Grid.CANNOT_CONVERT));
        String unsanitized = """
                <div>
                    %s <b>%s</b><br>
                    %s %s<br>
                    %s %s<br>
                    %s %d<br>
                    %s %s
                </div>
                """.formatted(
                getDescriptionCaptionSpan(getTranslation(I18n.PRODUCT_NAME)),
                book.getProductName(),
                getDescriptionCaptionSpan(getTranslation(I18n.PRICE)),
                Utils.convertToPresentation(book.getPrice(), converter),
                getDescriptionCaptionSpan(getTranslation(I18n.AVAILABILITY)),
                Utils.createAvailabilityIcon(book.getAvailability()),
                getDescriptionCaptionSpan(getTranslation(I18n.IN_STOCK)),
                book.getStockCount(),
                getDescriptionCaptionSpan(getTranslation(I18n.CATEGORIES)),
                formatCategories(book));
        return Utils.sanitize(unsanitized);
    }

    // Helper method to create a span with a caption
    private static String getDescriptionCaptionSpan(String caption) {
        assert caption != null : "Caption must not be null";

        return String.format("<span class='%s'>%s:</span> ",
                VaadinCreateTheme.BOOKVIEW_GRID_DESCRIPTIONCAPTION, caption);
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

    /**
     * Returns the id of the last edited product.
     *
     * @return int value.
     */
    public int getEdited() {
        return edited;
    }

    private LockedObjects getLockedBooks() {
        return LockedObjects.get();
    }
}
