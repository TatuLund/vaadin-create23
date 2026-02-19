package org.vaadin.tatu.vaadincreate.stats;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.vaadin.tatu.vaadincreate.AbstractUITest;
import org.vaadin.tatu.vaadincreate.VaadinCreateTheme;
import org.vaadin.tatu.vaadincreate.VaadinCreateUI;
import org.vaadin.tatu.vaadincreate.backend.data.Availability;
import org.vaadin.tatu.vaadincreate.backend.data.Category;
import org.vaadin.tatu.vaadincreate.backend.data.Product;
import org.vaadin.tatu.vaadincreate.backend.events.CategoriesUpdatedEvent;
import org.vaadin.tatu.vaadincreate.backend.events.CategoriesUpdatedEvent.CategoryChange;
import org.vaadin.tatu.vaadincreate.crud.BooksPresenter;
import org.vaadin.tatu.vaadincreate.crud.BooksView;
import org.vaadin.tatu.vaadincreate.crud.FakeGrid;
import org.vaadin.tatu.vaadincreate.eventbus.EventBus;
import org.vaadin.tatu.vaadincreate.common.CustomChart;

import com.vaadin.addon.charts.model.DataSeries;
import com.vaadin.server.ServiceException;
import com.vaadin.testbench.uiunittest.SerializationDebugUtil;
import com.vaadin.ui.CssLayout;

public class StatsViewTest extends AbstractUITest {

    private StatsView view;
    private VaadinCreateUI ui;
    private Map<String, Number> prices;
    private Map<String, Number> availabilities;
    private Map<String, Number> stockTitles;
    private Map<String, Number> stockCounts;
    private CssLayout dashboard;
    private Category category;

    @Before
    public void setup() throws ServiceException {
        ui = new VaadinCreateUI();
        mockVaadin(ui);
        login();

        view = navigate(StatsView.VIEW_NAME, StatsView.class);
        assertAssistiveNotification("Statistics opened");

        dashboard = $(CssLayout.class).styleName(VaadinCreateTheme.DASHBOARD)
                .first();
        waitForCharts(dashboard);

        category = new Category();
        category.setName("Empty");
        category = ui.getProductService().updateCategory(category);
    }

    @Test
    public void availability_chart_is_focused_after_navigating_to_view() {
        var chart = $(CustomChart.class).id("availability-chart");
        assertTrue(test(chart).isFocused());
    }

    @Test
    public void charts_will_be_updated_when_category_is_being_updated() {
        // Setup reference stats
        prices = referencePrices();
        availabilities = referenceAvailabilities();
        stockTitles = referenceStockTitles();
        stockCounts = referenceStockCounts();

        // Assert that initial stats are the same
        then_statistics_match_given_reference(prices, availabilities,
                stockTitles, stockCounts);

        // WHEN: other user renames the category "Sci-Fi" to "Science-Fiction"
        var sciFiCategory = ui.getProductService().getAllCategories().stream()
                .filter(cat -> cat.getName().equals("Sci-fi")).findFirst()
                .get();
        sciFiCategory.setName("Science-Fiction");
        sciFiCategory = ui.getProductService().updateCategory(sciFiCategory);
        // Fire event to update the stats
        EventBus.get().post(new CategoriesUpdatedEvent(sciFiCategory.getId(),
                CategoryChange.SAVE));
        waitForCharts(dashboard);

        // Update reference stats to reflect category change
        stockTitles.put("Science-Fiction", 26);
        stockTitles.remove("Sci-fi");
        stockCounts.put("Science-Fiction", 2321);
        stockCounts.remove("Sci-fi");

        // Assert that actual stats have been updated
        then_statistics_match_given_reference(prices, availabilities,
                stockTitles, stockCounts);

        // Clean-up, revert the category name
        sciFiCategory.setName("Sci-fi");
        ui.getProductService().updateCategory(sciFiCategory);
    }

    @Test
    public void chart_series_will_be_automatically_updated_when_book_update_and_delete_events_are_observed() {
        // Setup reference stats
        prices = referencePrices();
        availabilities = referenceAvailabilities();
        stockTitles = referenceStockTitles();
        stockCounts = referenceStockCounts();

        // Assert that initial stats are the same
        then_statistics_match_given_reference(prices, availabilities,
                stockTitles, stockCounts);

        var presenter = createBooksPresenter();

        // WHEN: other user is saving a new product
        var book = createBook();
        var savedBook = presenter.saveProduct(book);
        waitForCharts(dashboard);

        // Update reference stats to reflect new product
        prices.put("40 - 50 €", 1);
        availabilities.put("AVAILABLE", 29);
        book.getCategory().forEach(cat -> {
            stockTitles.put(cat.getName(),
                    stockTitles.get(cat.getName()).intValue() + 1);
        });
        book.getCategory().forEach(cat -> {
            stockCounts.put(cat.getName(),
                    stockCounts.get(cat.getName()).intValue() + 100);
        });

        // Assert that actual stats have been updated
        then_statistics_match_given_reference(prices, availabilities,
                stockTitles, stockCounts);

        // WHEN: other user is deleting the book
        presenter.deleteProduct(savedBook);
        waitForCharts(dashboard);

        // Revert the reference stats
        prices = referencePrices();
        availabilities = referenceAvailabilities();
        stockTitles = referenceStockTitles();
        stockCounts = referenceStockCounts();

        // Assert that actual stats are back to original
        then_statistics_match_given_reference(prices, availabilities,
                stockTitles, stockCounts);

    }

    @Test
    public void stats_view_is_serializable() {
        SerializationDebugUtil.assertSerializable(view);
    }

    private BooksPresenter createBooksPresenter() {
        var bookView = new BooksView();
        // It is not possible to have multiple parallel UIs in this thread
        view.addComponent(bookView);
        var presenter = new BooksPresenter(bookView);
        presenter.requestUpdateProducts();
        var fake = $(bookView, FakeGrid.class).first();
        waitWhile(fake, f -> f.isVisible(), 10);
        return presenter;
    }

    private void then_statistics_match_given_reference(
            Map<String, Number> prices, Map<String, Number> availabilities,
            Map<String, Number> stockTitles, Map<String, Number> stockCounts) {
        var chart = $(CustomChart.class).id("price-chart");
        var series = (DataSeries) chart.getConfiguration().getSeries().get(0);
        then_data_series_values_matches_given_reference_values(prices, series);

        chart = $(CustomChart.class).id("availability-chart");
        series = (DataSeries) chart.getConfiguration().getSeries().get(0);
        then_data_series_values_matches_given_reference_values(availabilities,
                series);

        chart = $(CustomChart.class).id("category-chart");
        series = (DataSeries) chart.getConfiguration().getSeries().get(0);
        then_data_series_values_matches_given_reference_values(stockTitles,
                series);

        series = (DataSeries) chart.getConfiguration().getSeries().get(1);
        then_data_series_values_matches_given_reference_values(stockCounts,
                series);
    }

    private static Product createBook() {
        var book = new Product();
        book.setProductName("Test");
        book.setAvailability(Availability.AVAILABLE);
        var categories = VaadinCreateUI.get().getProductService()
                .findCategoriesByIds(Set.of(1, 2));
        book.setCategory(categories);
        book.setStockCount(100);
        book.setPrice(BigDecimal.valueOf(45));
        return book;
    }

    private static void then_data_series_values_matches_given_reference_values(
            Map<String, Number> values, DataSeries series) {
        // Asserting size to verify that items with zero count are not included
        assertEquals(values.values().size(), series.size());
        series.getData().forEach(item -> {
            var name = item.getName();
            var y = item.getY();
            assertEquals(values.get(name), y.intValue());
        });
    }

    private static Map<String, Number> referencePrices() {
        var values = new HashMap<String, Number>();
        values.put("10 - 20 €", 40);
        values.put("20 - 30 €", 36);
        values.put("0 - 10 €", 24);
        return values;
    }

    private static Map<String, Number> referenceAvailabilities() {
        var values = new HashMap<String, Number>();
        values.put("DISCONTINUED", 34);
        values.put("COMING", 38);
        values.put("AVAILABLE", 28);
        return values;
    }

    private Map<String, Number> referenceStockCounts() {
        var values = new HashMap<String, Number>();
        values.put("Non-fiction", 622);
        values.put("Sci-fi", 2321);
        values.put("Thriller", 863);
        values.put("Romance", 1014);
        values.put("Mystery", 665);
        values.put("Cookbooks", 546);
        values.put("Children's books", 2082);
        values.put("Best sellers", 1267);
        return values;
    }

    private Map<String, Number> referenceStockTitles() {
        var values = new HashMap<String, Number>();
        values.put("Non-fiction", 15);
        values.put("Sci-fi", 26);
        values.put("Thriller", 14);
        values.put("Romance", 23);
        values.put("Mystery", 16);
        values.put("Cookbooks", 7);
        values.put("Children's books", 26);
        values.put("Best sellers", 22);
        return values;
    }

    @After
    public void cleanUp() {
        ui.getProductService().deleteCategory(category.getId());
        logout();
        tearDown();
    }
}
