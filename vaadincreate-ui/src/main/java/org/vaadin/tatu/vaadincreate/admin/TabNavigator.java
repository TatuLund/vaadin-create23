package org.vaadin.tatu.vaadincreate.admin;

import java.util.Objects;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.tatu.vaadincreate.admin.TabView.ViewChangeEvent;
import org.vaadin.tatu.vaadincreate.admin.TabView.ViewChangeListener;

import com.vaadin.server.AbstractErrorMessage.ContentMode;
import com.vaadin.server.Resource;
import com.vaadin.server.UserError;
import com.vaadin.shared.Registration;
import com.vaadin.shared.ui.ErrorLevel;
import com.vaadin.ui.ComponentContainer;
import com.vaadin.ui.Composite;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.UI;
import com.vaadin.ui.themes.ValoTheme;

/**
 * A tab navigator for sub-navigation.
 */
@NullMarked
@SuppressWarnings("java:S2160")
public class TabNavigator extends Composite {

    private static final Logger logger = LoggerFactory
            .getLogger(TabNavigator.class);

    private final TabSheet tabSheet = new TabSheet();

    @Nullable
    private TabView oldTabView = null;

    private final String baseViewName;

    /**
     * Constructs a new TabNavigator for sub-navigation.
     *
     * @param baseViewName
     *            The base view name of the view hosting this navigator
     */
    public TabNavigator(String baseViewName) {
        this.baseViewName = baseViewName;
        tabSheet.setSizeFull();
        tabSheet.addStyleNames(ValoTheme.TABSHEET_PADDED_TABBAR,
                ValoTheme.TABSHEET_CENTERED_TABS);
        tabSheet.addSelectedTabChangeListener(tabChange -> {
            var selectedTab = (TabView) tabSheet.getSelectedTab();
            String tabName = null;
            tabName = selectedTab.getTabName();
            setFragmentParameter(tabName);
            var tabViewChange = new TabView.ViewChangeEvent(this, oldTabView,
                    selectedTab, tabName);
            selectedTab.enter(tabViewChange);
            fireEvent(tabViewChange);
            logger.info("Tab '{}' selected.", tabName);
            oldTabView = (TabView) tabSheet.getSelectedTab();
        });
        setCompositionRoot(tabSheet);
    }

    /**
     * Adds a view change listener to this navigator.
     * 
     * @param listener
     *            The listener to add
     * @return A registration for removing the listener later
     */
    public Registration addViewChangeListener(ViewChangeListener listener) {
        return addListener(ViewChangeEvent.class, listener,
                ViewChangeListener.VIEW_CHANGED_METHOD);
    }

    /**
     * Adds a tab view to the navigator.
     * 
     * @param tabView
     *            The tab view to add
     * @param caption
     *            The caption of the tab
     * @param icon
     *            The icon of the tab
     */
    public void addTabView(TabView tabView, String caption, Resource icon) {
        Objects.requireNonNull(tabView, "TabView cannot be null");
        Objects.requireNonNull(caption, "Caption cannot be null");
        Objects.requireNonNull(icon, "Icon cannot be null");
        if (!(tabView instanceof ComponentContainer)) {
            throw new IllegalArgumentException(
                    "TabView must be a ComponentContainer");
        }
        var tab = tabSheet.addTab(tabView, caption);
        tab.setIcon(icon);
    }

    /**
     * Navigate to the given tab view.
     * 
     * @param tabView
     *            The tab view to navigate to
     */
    public void navigate(TabView tabView) {
        Objects.requireNonNull(tabView, "TabView cannot be null");
        oldTabView = (TabView) tabSheet.getSelectedTab();
        tabSheet.setSelectedTab(tabView);
    }

    /**
     * Navigate to the tab view with the given name.
     * 
     * @param viewName
     *            The name of the tab view to navigate to
     */
    public void navigate(String viewName) {
        Objects.requireNonNull(viewName, "View name cannot be null");
        var count = tabSheet.getComponentCount();
        for (int i = 0; i < count; i++) {
            var tabView = (TabView) tabSheet.getTab(i).getComponent();
            if (tabView.getTabName().equals(viewName)) {
                navigate(tabView);
                return;
            }
        }
        throw new IllegalArgumentException(
                "No tab with view name: " + viewName);
    }

    /**
     * Clear component error on the given tab view.
     */
    public void clearComponentError(TabView view) {
        tabSheet.getTab(view).setComponentError(null);
    }

    /**
     * Set component error on the given tab view.
     *
     * @param view
     *            The view to set error on
     * @param errorText
     *            The error text to show
     */
    public void setComponentError(TabView view, String errorText) {
        var errorMessage = new UserError(errorText, ContentMode.TEXT,
                ErrorLevel.WARNING);
        tabSheet.getTab(view).setComponentError(errorMessage);
    }

    /**
     * Update the fragment without causing navigator to change view
     *
     * @param tabName
     *            The name of the tab to show
     */
    private void setFragmentParameter(String tabName) {
        String fragmentParameter = tabName;
        var page = UI.getCurrent().getPage();
        page.setUriFragment(
                String.format("!%s/%s", baseViewName, fragmentParameter),
                false);
    }
}
