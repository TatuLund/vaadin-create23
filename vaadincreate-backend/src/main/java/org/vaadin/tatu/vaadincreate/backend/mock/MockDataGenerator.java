package org.vaadin.tatu.vaadincreate.backend.mock;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.vaadin.tatu.vaadincreate.backend.data.Availability;
import org.vaadin.tatu.vaadincreate.backend.data.Address;
import org.vaadin.tatu.vaadincreate.backend.data.Category;
import org.vaadin.tatu.vaadincreate.backend.data.Product;
import org.vaadin.tatu.vaadincreate.backend.data.Purchase;
import org.vaadin.tatu.vaadincreate.backend.data.PurchaseLine;
import org.vaadin.tatu.vaadincreate.backend.data.PurchaseStatus;
import org.vaadin.tatu.vaadincreate.backend.data.User;
import org.vaadin.tatu.vaadincreate.backend.data.User.Role;

@NullMarked
@SuppressWarnings("serial")
public class MockDataGenerator implements Serializable {
    private static final Random random = new Random(1);

    @NonNull
    private static final String[] categoryNames = new @NonNull String[] {
            "Children's books", "Best sellers", "Romance", "Mystery",
            "Thriller", "Sci-fi", "Non-fiction", "Cookbooks" };

    private static @NonNull String[] word1 = new @NonNull String[] {
            "The art of", "Mastering", "The secrets of", "Avoiding",
            "For fun and profit: ", "How to fail at",
            "10 important facts about", "The ultimate guide to", "Book of",
            "Surviving", "Encyclopedia of", "Very much",
            "Learning the basics of", "The cheap way to", "Being awesome at",
            "The life changer:", "The Vaadin way:", "Becoming one with",
            "Beginners guide to", "The complete visual guide to",
            "The mother of all references:" };

    private static @NonNull String[] word2 = new @NonNull String[] {
            "gardening", "living a healthy life", "designing tree houses",
            "home security", "intergalaxy travel", "meditation", "ice hockey",
            "children's education", "computer programming", "Vaadin TreeTable",
            "winter bathing", "playing the cello", "dummies", "rubber bands",
            "feeling down", "debugging", "running barefoot",
            "speaking to a big audience", "creating software", "giant needles",
            "elephants", "keeping your wife happy" };

    public static List<@NonNull Category> createCategories() {
        List<@NonNull Category> categories = new ArrayList<>();
        for (@NonNull
        String name : categoryNames) {
            Category c = createCategory(name);
            categories.add(c);
        }
        return categories;
    }

    public static List<@NonNull Product> createProducts(
            List<@NonNull Category> categories) {
        List<@NonNull Product> products = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            Product p = createProduct(categories);
            products.add(p);
        }

        return products;
    }

    @SuppressWarnings("java:S3400")
    public static String createMessage() {
        return "System update complete";
    }

    public static List<@NonNull User> createUsers() {
        List<@NonNull User> users = new ArrayList<>();
        for (Integer i = 0; i < 10; i++) {
            User user = new User();
            user.setName("User" + i);
            user.setPasswd("user" + i);
            user.setRole(Role.USER);
            users.add(user);
        }
        User admin = new User();
        admin.setName("Admin");
        admin.setPasswd("admin");
        admin.setRole(Role.ADMIN);
        users.add(admin);
        admin = new User();
        admin.setName("Super");
        admin.setPasswd("super");
        admin.setRole(Role.ADMIN);
        users.add(admin);
        // Create 100 CUSTOMER users for testing storefront functionality
        for (Integer i = 0; i < 100; i++) {
            User customer = new User();
            customer.setName("Customer" + i);
            customer.setPasswd("customer" + i);
            customer.setRole(Role.CUSTOMER);
            users.add(customer);
        }
        return users;
    }

    private static Category createCategory(String name) {
        Category c = new Category();
        c.setName(name);
        return c;
    }

    private static Product createProduct(List<Category> categories) {
        Product p = new Product();
        p.setProductName(generateName());

        var price = getPrice();
        p.setPrice(price);
        p.setAvailability(Availability.values()[random
                .nextInt(Availability.values().length)]);
        if (p.getAvailability() == Availability.AVAILABLE) {
            p.setStockCount(random.nextInt(523));
        }

        p.setCategory(getCategory(categories, 1, 2));
        return p;
    }

    @SuppressWarnings("null")
    private static BigDecimal getPrice() {
        return BigDecimal.valueOf((random.nextInt(250) + 50) / 10.0);
    }

    private static Set<Category> getCategory(List<Category> categories, int min,
            int max) {
        int nr = random.nextInt(max) + min;
        HashSet<Category> productCategories = new HashSet<>();
        for (int i = 0; i < nr; i++) {
            productCategories
                    .add(categories.get(random.nextInt(categories.size())));
        }

        return productCategories;
    }

    private static String generateName() {
        return word1[random.nextInt(word1.length)] + " "
                + word2[random.nextInt(word2.length)];
    }

    /**
     * Creates fabricated Purchase data for UX testing.
     * <p>
     * Generates exactly 1600 purchases, starting from the current date
     * (inclusive), two purchases per date. Requesters are taken from the
     * provided {@code customers} list (round-robin). Approvers are randomly
     * chosen from {@code approvers}. Each purchase gets 1-3 lines with
     * quantities 1-5.
     *
     * @param customers
     *            requesters (e.g. Customer11..Customer20)
     * @param approvers
     *            approvers (e.g. User5/User6)
     * @param products
     *            products to pick from
     * @return list of 1600 transient Purchase entities
     */
    public static List<@NonNull Purchase> createMockPurchases(
            List<@NonNull User> customers,
            List<@NonNull User> approvers,
            List<@NonNull Product> products) {
        if (customers.isEmpty()) {
            throw new IllegalArgumentException("customers must not be empty");
        }
        if (approvers.isEmpty()) {
            throw new IllegalArgumentException("approvers must not be empty");
        }
        if (products.isEmpty()) {
            throw new IllegalArgumentException("products must not be empty");
        }

        final int purchasesPerDate = 2;
        final int totalPurchases = 1600;
        final int dateCount = totalPurchases / purchasesPerDate;

        List<@NonNull Purchase> purchases = new ArrayList<>(totalPurchases);
        LocalDate today = LocalDate.now();
        ZoneId zone = ZoneId.systemDefault();

        for (int dayIndex = 0; dayIndex < dateCount; dayIndex++) {
            LocalDate date = today.minusDays(dayIndex);
            for (int i = 0; i < purchasesPerDate; i++) {
                Purchase purchase = new Purchase();

                var requester = customers.get(
                        (dayIndex * purchasesPerDate + i)
                                % customers.size());
                purchase.setRequester(requester);

                var approver = approvers.get(random.nextInt(approvers.size()));
                purchase.setApprover(approver);

                PurchaseStatus status = PurchaseStatus
                        .values()[random
                                .nextInt(PurchaseStatus.values().length)];
                purchase.setStatus(status);

                // Two distinct times per day to make ordering stable.
                LocalDateTime created = date.atTime(i == 0 ? 9 : 15,
                        random.nextInt(60));
                var createdAt = created.atZone(zone).toInstant();
                purchase.setCreatedAt(createdAt);

                if (status != PurchaseStatus.PENDING) {
                    // For decided purchases, set decision metadata.
                    var decidedAt = created.plusHours(1 + random.nextInt(72))
                            .atZone(zone).toInstant();
                    purchase.setDecidedAt(decidedAt);
                    purchase.setDecisionReason("Mock decision");
                }

                purchase.setDeliveryAddress(new Address(
                        "Mock Street " + (dayIndex + 1),
                        String.format("%05d", 10000 + (dayIndex % 9000)),
                        "Mock City",
                        "Mock Country"));

                int lineCount = 1 + random.nextInt(3);
                for (int lineIndex = 0; lineIndex < lineCount; lineIndex++) {
                    Product product = products
                            .get(random.nextInt(products.size()));
                    int quantity = 1 + random.nextInt(5);

                    PurchaseLine line = new PurchaseLine();
                    line.setProduct(product);
                    line.setQuantity(quantity);
                    line.setUnitPrice(product.getPrice());
                    purchase.addLine(line);
                }

                purchases.add(purchase);
            }
        }

        return purchases;
    }

}
