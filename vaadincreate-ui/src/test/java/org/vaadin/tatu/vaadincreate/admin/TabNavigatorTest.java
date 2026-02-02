package org.vaadin.tatu.vaadincreate.admin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.icons.VaadinIcons;
import com.vaadin.server.ServiceException;
import com.vaadin.testbench.uiunittest.UIUnitTest;
import com.vaadin.ui.Label;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

@SuppressWarnings("null")
public class TabNavigatorTest extends UIUnitTest {

    private TabNavigator tabNavigator;
    private UI ui;
    private TestView1 view1;
    private TestView2 view2;

    @Before
    public void setup() throws ServiceException {
        ui = mockVaadin();
        tabNavigator = new TabNavigator("");
        view1 = new TestView1();
        tabNavigator.addTabView(view1, "Test View 1", VaadinIcons.HOME);
        view2 = new TestView2();
        tabNavigator.addTabView(view2, "Test View 2", VaadinIcons.HOME);
        ui.setContent(tabNavigator);
    }

    @After
    public void teardown() {
        tearDown();
    }

    @Test
    public void testSetFragmentParameter() {
        tabNavigator.navigate("test2");
        assertEquals("!/test2", ui.getPage().getUriFragment());
        tabNavigator.navigate("test1");
        assertEquals("!/test1", ui.getPage().getUriFragment());
    }

    @Test
    public void testViewChangeListener() {
        final boolean[] listenerCalled = { false };
        tabNavigator.addViewChangeListener(event -> {
            listenerCalled[0] = true;
            assertEquals("test2", event.getNewView().getTabName());
            assertEquals("test1", event.getOldView().getTabName());
        });
        tabNavigator.navigate("test2");
        assertTrue(listenerCalled[0]);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalTabNavigationThrows() {
        tabNavigator.navigate("nonexistent");
    }

    @Test
    public void testComponentError() {
        tabNavigator.setComponentError(view1, "Error text");
        var tabSheet = $(TabSheet.class).first();
        var error = tabSheet.getTab(view1).getComponentError();
        assertEquals("Error&#32;text", error.getFormattedHtmlMessage());
        tabNavigator.clearComponentError(view1);
        assertNull(tabSheet.getTab(view1).getComponentError());
    }

    @Test
    public void testEnterCalledOnView() {
        tabNavigator.navigate("test2");
        assertTrue(view2.isEnterCalled());
    }

    @Test
    public void testEnterNotCalledOnSameView() {
        tabNavigator.navigate("test1");
        assertTrue(!view2.isEnterCalled());
    }

    @Test
    public void testInitialViewIsFirstTab() {
        var tabSheet = $(TabSheet.class).first();
        var selectedTab = tabSheet.getSelectedTab();
        assertEquals(view1, selectedTab);
    }

    @Test(expected = IllegalArgumentException.class)
    public void addTabView_withNonContainerTabView_throwsIllegalArgumentException() {
        var nonContainerView = new NonContainerTabView();
        tabNavigator.addTabView(nonContainerView, "NonContainer",
                VaadinIcons.HOME);
    }

    public static class TestView1 extends VerticalLayout implements TabView {

        @Override
        public String getTabName() {
            return "test1";
        }

        @Override
        public void enter(ViewChangeEvent event) {
            // No-op
        }
    }

    public static class TestView2 extends VerticalLayout implements TabView {

        private boolean enterCalled;

        @Override
        public String getTabName() {
            return "test2";
        }

        @Override
        public void enter(ViewChangeEvent event) {
            this.enterCalled = true;
        }

        public boolean isEnterCalled() {
            return enterCalled;
        }
    }

    public static class NonContainerTabView extends Label implements TabView {

        @Override
        public String getTabName() {
            return "nonContainer";
        }

        @Override
        public void enter(ViewChangeEvent event) {
            // No-op
        }
    }

}
