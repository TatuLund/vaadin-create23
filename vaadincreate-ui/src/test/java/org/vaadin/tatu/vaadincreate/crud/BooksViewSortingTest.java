package org.vaadin.tatu.vaadincreate.crud;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class BooksViewSortingTest extends AbstractBooksViewTest {

    @Test
    public void clicking_sorting_grid_by_price_will_sort_ascending_second_click_descending() {
        int size = test(grid).size();

        // WHEN: Clicking price column sorting toggle
        test(grid).toggleColumnSorting(2);

        // THEN: Grid is sorted by price in ascending order
        for (int i = 1; i < size; i++) {
            var result = test(grid).item(i - 1).getPrice()
                    .compareTo(test(grid).item(i).getPrice());
            assertTrue(result <= 0);
        }

        // WHEN: Clicking price column sorting toggle again
        test(grid).toggleColumnSorting(2);

        // THEN: Grid is sorted by price in descending order
        for (int i = 1; i < size; i++) {
            var result = test(grid).item(i - 1).getPrice()
                    .compareTo(test(grid).item(i).getPrice());
            assertTrue(result >= 0);
        }
    }

    @Test
    public void clicking_sorting_grid_by_name_will_sort_ascending_second_click_descending() {
        int size = test(grid).size();

        // WHEN: Clicking name column sorting toggle
        test(grid).toggleColumnSorting(1);

        // THEN: Grid is sorted by name in alphabetically ascending
        // order
        for (int i = 1; i < size; i++) {
            var result = ((String) test(grid).cell(1, i - 1))
                    .compareToIgnoreCase((String) test(grid).cell(1, i));
            assertTrue(result <= 0);
        }

        // WHEN: Clicking name column sorting toggle again
        test(grid).toggleColumnSorting(1);

        // THEN: Grid is sorted by name in alphabetically descending
        // order
        for (int i = 1; i < size; i++) {
            var result = ((String) test(grid).cell(1, i - 1))
                    .compareToIgnoreCase((String) test(grid).cell(1, i));
            assertTrue(result >= 0);
        }
    }
}
