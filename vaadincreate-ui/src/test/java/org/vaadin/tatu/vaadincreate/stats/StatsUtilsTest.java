package org.vaadin.tatu.vaadincreate.stats;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.vaadin.tatu.vaadincreate.backend.data.Availability;
import org.vaadin.tatu.vaadincreate.backend.data.Category;
import org.vaadin.tatu.vaadincreate.backend.data.Product;
import org.vaadin.tatu.vaadincreate.stats.StatsUtils.PriceBracket;

public class StatsUtilsTest {
    private List<Product> products;
    private Product product1;
    private Product product2;
    private Category category1;
    private Category category2;

    @Before
    public void setUp() {
        products = new ArrayList<>();
        category1 = new Category();
        category1.setName("Category 1");
        category2 = new Category();
        category2.setName("Category 2");

        product1 = new Product();
        product1.setProductName("Product 1");
        product1.setPrice(new BigDecimal("15.00"));
        product1.setStockCount(10);
        product1.setAvailability(Availability.AVAILABLE);
        product1.setCategory(Set.of(category1));

        product2 = new Product();
        product2.setProductName("Product 2");
        product2.setPrice(new BigDecimal("25.00"));
        product2.setStockCount(5);
        product2.setAvailability(Availability.DISCONTINUED);
        product2.setCategory(Set.of(category2));

        products.add(product1);
        products.add(product2);
    }

    @Test
    public void calculatePriceStats_WithProducts_ReturnsPriceBracketCounts() {
        Map<String, Long> stats = StatsUtils.calculatePriceStats(products);

        assertEquals("Expected 2 brackets", 2, stats.size());
        assertEquals("Expected 1 product in 10-20€ bracket", 1L,
                stats.get("10 - 20 €").longValue());
        assertEquals("Expected 1 product in 20-30€ bracket", 1L,
                stats.get("20 - 30 €").longValue());

        var product = new Product();
        product.setPrice(new BigDecimal("9.99"));
        product.setAvailability(Availability.AVAILABLE);
        product.setCategory(Set.of(category1));
        products.add(product);
        stats = StatsUtils.calculatePriceStats(products);
        assertEquals("Expected 3 brackets after adding product", 3,
                stats.size());
        assertEquals("Expected 1 product in 0-10€ bracket", 1L,
                stats.get("0 - 10 €").longValue());

        product = new Product();
        product.setPrice(new BigDecimal("9.00"));
        product.setAvailability(Availability.AVAILABLE);
        product.setCategory(Set.of(category1));
        products.add(product);
        stats = StatsUtils.calculatePriceStats(products);
        assertEquals("Expected 3 brackets after adding product", 3,
                stats.size());
        assertEquals("Expected 2 products in 0-10€ bracket", 2L,
                stats.get("0 - 10 €").longValue());
    }

    @Test
    public void calculatePriceStats_WithEmptyList_ReturnsEmptyMap() {
        Map<String, Long> stats = StatsUtils
                .calculatePriceStats(new ArrayList<>());
        assertTrue("Expected empty stats map", stats.isEmpty());
    }

    @Test
    public void calculateCategoryStats_WithProducts_ReturnsCategoryCounts() {
        List<Category> categories = List.of(category1, category2);
        Map<String, Long[]> stats = StatsUtils
                .calculateCategoryStats(categories, products);

        assertEquals("Expected 2 categories", 2, stats.size());
        assertArrayEquals("Invalid stats for Category 1",
                new Long[] { 1L, 10L }, stats.get("Category 1"));
        assertArrayEquals("Invalid stats for Category 2", new Long[] { 1L, 5L },
                stats.get("Category 2"));

        var category3 = new Category();
        category3.setName("Category 3");
        categories = List.of(category1, category2, category3);
        product1.setCategory(Set.of(category1, category3));
        stats = StatsUtils.calculateCategoryStats(categories, products);
        assertEquals("Expected 3 categories after adding Category 3", 3,
                stats.size());
        assertArrayEquals(
                "Invalid stats for Category 1 after adding Category 3",
                new Long[] { 1L, 10L }, stats.get("Category 1"));
        assertArrayEquals(
                "Invalid stats for Category 3 after adding Category 3",
                new Long[] { 1L, 10L }, stats.get("Category 3"));

        var product3 = new Product();
        product3.setProductName("Product 3");
        product3.setPrice(new BigDecimal("5.00"));
        product3.setStockCount(2);
        product3.setAvailability(Availability.AVAILABLE);
        product3.setCategory(Set.of(category3));
        products.add(product3);

        stats = StatsUtils.calculateCategoryStats(categories, products);
        assertEquals("Expected 3 categories after adding Product 3", 3,
                stats.size());
        assertArrayEquals("Invalid stats for Category 3 after adding Product 3",
                new Long[] { 2L, 12L }, stats.get("Category 3"));
        assertArrayEquals("Invalid stats for Category 1 after adding Product 3",
                new Long[] { 1L, 10L }, stats.get("Category 1"));
    }

    @Test
    public void calculateAvailabilityStats_WithProducts_ReturnsAvailabilityCounts() {
        Map<Availability, Long> stats = StatsUtils
                .calculateAvailabilityStats(products);

        assertEquals("Expected all availability statuses",
                Availability.values().length, stats.size());
        assertEquals("Expected 1 AVAILABLE product", 1L,
                stats.get(Availability.AVAILABLE).longValue());
        assertEquals("Expected 1 DISCONTINUED product", 1L,
                stats.get(Availability.DISCONTINUED).longValue());
        assertEquals("Expected 0 COMING products", 0L,
                stats.get(Availability.COMING).longValue());

        var product3 = new Product();
        product3.setProductName("Product 3");
        product3.setPrice(new BigDecimal("5.00"));
        product3.setStockCount(2);
        product3.setAvailability(Availability.COMING);
        product3.setCategory(Set.of(category1));
        products.add(product3);

        stats = StatsUtils.calculateAvailabilityStats(products);
        assertEquals("Expected all availability statuses after adding Product 3",
                Availability.values().length, stats.size());
        assertEquals("Expected 1 COMING product after adding Product 3", 1L,
                stats.get(Availability.COMING).longValue());
        assertEquals("Expected 1 AVAILABLE product after adding Product 3", 1L,
                stats.get(Availability.AVAILABLE).longValue());
        assertEquals("Expected 1 DISCONTINUED product after adding Product 3", 1L,
                stats.get(Availability.DISCONTINUED).longValue());

        var product4 = new Product();
        product4.setProductName("Product 4");
        product4.setPrice(new BigDecimal("8.00"));
        product4.setStockCount(3);
        product4.setAvailability(Availability.AVAILABLE);
        product4.setCategory(Set.of(category1));
        products.add(product4);

        stats = StatsUtils.calculateAvailabilityStats(products);
        assertEquals("Expected all availability statuses after adding Product 4",
                Availability.values().length, stats.size());
        assertEquals("Expected 2 AVAILABLE products after adding Product 4", 2L,
                stats.get(Availability.AVAILABLE).longValue());
        assertEquals("Expected 1 COMING product after adding Product 4", 1L,
                stats.get(Availability.COMING).longValue());
        assertEquals("Expected 1 DISCONTINUED product after adding Product 4", 1L,
                stats.get(Availability.DISCONTINUED).longValue());
    }

    @Test
    public void getPriceBrackets_WithProducts_ReturnsCorrectBrackets() {
        List<PriceBracket> brackets = StatsUtils.getPriceBrackets(products);

        assertEquals("Expected 3 brackets", 3, brackets.size());
        assertEquals("First bracket max should be 10", 10,
                brackets.get(0).max());
        assertEquals("Second bracket max should be 20", 20,
                brackets.get(1).max());
        assertEquals("Third bracket max should be 30", 30,
                brackets.get(2).max());
    }

    @Test
    public void getPriceBrackets_WithEmptyList_ReturnsEmptyList() {
        List<PriceBracket> brackets = StatsUtils
                .getPriceBrackets(new ArrayList<>());
        assertTrue("Expected empty brackets list", brackets.isEmpty());
    }

    @Test
    public void priceBracket_IsInPriceBracket_ReturnsCorrectResult() {
        PriceBracket bracket = new PriceBracket(20);

        assertTrue("15.00 should be in the 10-20€ bracket",
                bracket.isInPriceBracket(new BigDecimal("15.00")));
        assertFalse("20.00 should not be in the 10-20€ bracket",
                bracket.isInPriceBracket(new BigDecimal("20.00")));
        assertFalse("9.99 should not be in the 10-20€ bracket",
                bracket.isInPriceBracket(new BigDecimal("9.99")));
    }

    @Test
    public void priceBracket_ToString_ReturnsFormattedString() {
        PriceBracket bracket = new PriceBracket(20);
        assertEquals("Expected formatted price bracket string", "10 - 20 €",
                bracket.toString());
    }
}
