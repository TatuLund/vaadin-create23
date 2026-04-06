package org.vaadin.tatu.vaadincreate.purchases;

import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.After;
import org.junit.Test;
import org.vaadin.tatu.vaadincreate.AbstractViewTest;

import com.deque.html.axecore.selenium.AxeBuilder;

public class PurchasesHistoryViewIT extends AbstractViewTest {

    @Override
    public void setup() {
        super.setup();
        open("#!" + PurchasesView.VIEW_NAME + "/"
                + PurchasesHistoryView.VIEW_NAME);
        login("Admin", "admin");
    }

    @Test
    public void accessibility() {
        var axeBuilder = new AxeBuilder();
        // Vaadin tooltip is aria-live and is populated when hovering over
        // elements. Thus working differently than Axe assumes, but works on
        // NVDA properly.
        axeBuilder.exclude(".v-tooltip");
        // The first column contains the expand/collapse togge, header is
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