package org.vaadin.tatu.vaadincreate.purchases;

import static org.junit.Assert.assertTrue;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.junit.After;
import org.junit.Test;
import org.vaadin.tatu.vaadincreate.AbstractViewTest;

import com.deque.html.axecore.selenium.AxeBuilder;
import com.vaadin.testbench.By;
import com.vaadin.testbench.elements.DateFieldElement;
import com.vaadin.testbench.elements.GridElement;

public class PurchasesHistoryViewIT extends AbstractViewTest {

    @Override
    public void setup() {
        super.setup();
        open("#!" + PurchasesView.VIEW_NAME + "/"
                + PurchasesHistoryView.VIEW_NAME);
        login("Admin", "admin");
    }

    @Test
    public void gridScrollsToExportRangeWhenDatesSet() {
        var fromDateField = $(DateFieldElement.class).id("export-from-date");
        var toDateField = $(DateFieldElement.class).id("export-to-date");
        var toDate = LocalDate.now().minusMonths(1);
        fromDateField.setDate(LocalDate.now().minusMonths(2));
        toDateField.setDate(toDate);

        var grid = $(GridElement.class).first();
        // Format date in Finnish Locale using "." as separator to match the
        // grid cell
        // content
        String searchDate = toDate
                .format(DateTimeFormatter.ofPattern("d.M.yyyy"));
        assertTrue(cellsContaining(grid, searchDate) > 0);
    }

    private long cellsContaining(GridElement grid, String text) {
        return grid.findElements(By.className("v-grid-cell")).stream()
                .filter(element -> element.getAttribute("innerHTML")
                        .contains(text))
                .count();
    }

    @Test
    public void accessibility() {
        var axeBuilder = new AxeBuilder();
        // Vaadin tooltip is aria-live and is populated when hovering over
        // elements. Thus working differently than Axe assumes, but works on
        // NVDA properly.
        axeBuilder.exclude(".v-tooltip");
        // The first column contains the expand/collapse toggle, header is
        // intentionally blank and thus excluded.
        axeBuilder.exclude("th:nth-child(1)");
        // DateField is autocomplete="off"
        // The popup assistive labels not visible, thus not in region
        axeBuilder.disableRules(List.of("autocomplete-valid", "region"));

        var axeResults = axeBuilder.analyze(driver);
        logViolations(axeResults);
        assertTrue(axeResults.violationFree());
    }

    @After
    public void cleanup() {
        logout();
    }

}