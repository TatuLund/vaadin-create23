package org.vaadin.tatu.vaadincreate.stats;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.jspecify.annotations.NullMarked;
import org.vaadin.tatu.vaadincreate.backend.data.Availability;
import org.vaadin.tatu.vaadincreate.backend.data.Category;
import org.vaadin.tatu.vaadincreate.backend.data.Product;

@NullMarked
public class StatsUtils {
    private static final String PRODUCTS_NOT_NULL = "Products must not be null";

    private StatsUtils() {
        // Utility class, no instantiation
    }

    /**
     * Calculate statistics based on product data and return as a map of price
     * brackets and product counts.
     *
     * @param products
     *            the collection of products to analyze
     * @return a map where keys are price bracket strings (e.g., "10 - 20 €")
     *         and values are the counts of products in those brackets
     */
    public static Map<String, Long> calculatePriceStats(
            Collection<Product> products) {
        var priceStats = getPriceBrackets(products).stream()
                .collect(Collectors.toMap(PriceBracket::toString,
                        priceBracket -> products.stream()
                                .filter(product -> priceBracket
                                        .isInPriceBracket(product.getPrice()))
                                .count()));
        // filter out empty price brackets
        priceStats.entrySet().removeIf(entry -> entry.getValue() == 0);
        return priceStats;
    }

    /**
     * Calculate statistics based on product data and return as a map of
     * category names and product counts.
     *
     * @param categories
     *            the collection of categories to analyze
     * @param products
     *            the collection of products to analyze
     * @return a map where keys are category names and values are arrays
     *         containing the counts of products in those categories
     */
    public static Map<String, Long[]> calculateCategoryStats(
            Collection<Category> categories, Collection<Product> products) {
        var categoryStats = categories.stream()
                .collect(Collectors.toMap(Category::getName, category -> {
                    long titles = products.stream().filter(
                            product -> product.getCategory().contains(category))
                            .count();
                    long instock = products.stream().filter(
                            product -> product.getCategory().contains(category))
                            .mapToLong(Product::getStockCount).sum();
                    return new Long[] { titles, instock };
                }));
        // filter out categories with zero products
        categoryStats.entrySet().removeIf(entry -> entry.getValue()[0] == 0);
        return categoryStats;
    }

    /**
     * Calculate statistics based on product data and return as a map of
     * availability types and product counts.
     *
     * @param products
     *            the collection of products to analyze
     * @return a map where keys are availability types and values are the counts
     *         of products in those categories
     */
    public static Map<Availability, Long> calculateAvailabilityStats(
            Collection<Product> products) {
        return Arrays.stream(Availability.values()).map(availability -> Map
                .entry(availability, products.stream().filter(
                        product -> product.getAvailability() == availability)
                        .count()))
                .collect(toEnumMap(Availability.class));
    }

    // SonarLint is not able to deduct from isEmpty() check that stream cannot
    // be empty and hence get() is safe
    @SuppressWarnings("java:S3655")
    static List<PriceBracket> getPriceBrackets(Collection<Product> products) {
        assert products != null : PRODUCTS_NOT_NULL;

        var brackets = new ArrayList<PriceBracket>();
        if (products.isEmpty()) {
            return brackets;
        }

        var max = products.stream()
                .max((p1, p2) -> p1.getPrice().compareTo(p2.getPrice())).get()
                .getPrice();
        var numBrackets = (max.intValue() / 10) + 1;
        for (int i = 10; i <= (numBrackets * 10); i += 10) {
            brackets.add(new PriceBracket(i));
        }
        return brackets;
    }

    // Collectors.toMap does not support EnumMap, so we need to implement our
    // own collector
    static <K extends Enum<K>, V> Collector<Map.Entry<K, V>, ?, EnumMap<K, V>> toEnumMap(
            Class<K> keyType) {
        return Collector.of(() -> new EnumMap<>(keyType),
                (map, entry) -> map.put(entry.getKey(), entry.getValue()),
                (map1, map2) -> {
                    map1.putAll(map2);
                    return map1;
                });
    }

    /**
     * Represents a price bracket where {@code max} is the upper bound
     * (exclusive) and the lower bound is always {@code max - 10} (inclusive).
     */
    record PriceBracket(int max) implements Serializable {
        public boolean isInPriceBracket(BigDecimal price) {
            BigDecimal upper = BigDecimal.valueOf(max);
            BigDecimal lower = BigDecimal.valueOf(max - 10L);
            return price.compareTo(upper) < 0 && price.compareTo(lower) >= 0;
        }

        @Override
        public String toString() {
            return (max - 10) + " - " + max + " €";
        }
    }
}
