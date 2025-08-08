package org.vaadin.tatu.vaadincreate.admin;

import java.util.Collection;
import java.util.Objects;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.vaadin.tatu.vaadincreate.AttributeExtension;
import org.vaadin.tatu.vaadincreate.VaadinCreateTheme;

import com.vaadin.data.ValueProvider;
import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.shared.ui.grid.HeightMode;
import com.vaadin.ui.Component;
import com.vaadin.ui.Composite;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Grid.SelectionMode;
import com.vaadin.ui.Panel;
import com.vaadin.ui.themes.ValoTheme;

/**
 * A list of components generated from a collection of items. Components are
 * shown as scrollable list.
 */
@NullMarked
@SuppressWarnings({ "serial", "java:S2160" })
public class ComponentList<T, V extends Component> extends Composite {

    private Grid<T> grid;
    @Nullable
    private ListDataProvider<T> dataProvider;
    private Panel panel = new Panel();
    private ValueProvider<T, V> componentProvider;

    /**
     * Creates a new instance of ComponentList.
     *
     * @param provider
     *            ValueProvider to create components from items, not null
     */
    public ComponentList(ValueProvider<T, V> provider) {
        Objects.requireNonNull(provider, "ValueProvider cannot be null");
        this.componentProvider = provider;
        grid = createGrid(provider);
        panel.setSizeFull();
        panel.setContent(grid);
        panel.addStyleName(ValoTheme.PANEL_BORDERLESS);
        setCompositionRoot(panel);
    }

    /**
     * Replaces an item in the list with a new item and regenerates the
     * component.
     *
     * @param removed
     *            item to be removed
     * @param added
     *            item to be added
     */
    public void replaceItem(T removed, T added) {
        throwIfDataProviderNotSet();
        dataProvider.getItems().remove(removed);
        dataProvider.getItems().add(added);
        dataProvider.refreshAll();
    }

    /**
     * Removes the specified item from the data provider, refreshes the data
     * provider, and adjusts the grid height based on the number of remaining
     * items.
     *
     * @param item
     *            the item to be removed from the data provider
     */
    public void removeItem(T item) {
        throwIfDataProviderNotSet();
        dataProvider.getItems().remove(item);
        dataProvider.refreshAll();
        grid.setHeightByRows(dataProvider.getItems().size());
    }

    /**
     * Sets the items to be shown in the list. And generates components from the
     * items.
     *
     * @param items
     *            Collection of items, not null
     */
    public void setItems(Collection<T> items) {
        dataProvider = new ListDataProvider<>(items);
        grid.setDataProvider(dataProvider);
        if (!items.isEmpty()) {
            grid.setHeightByRows(items.size());
        } else {
            grid.setHeightByRows(1);
        }
    }

    /**
     * Adds an item to the list and generates a component from the item.
     *
     * @param item
     *            item to be added, not null
     */
    public void addItem(T item) {
        throwIfDataProviderNotSet();
        dataProvider.getItems().add(item);
        dataProvider.refreshAll();
        grid.setHeightByRows(dataProvider.getItems().size());
        grid.scrollToEnd();
    }

    private void throwIfDataProviderNotSet() {
        if (dataProvider == null) {
            throw new IllegalStateException("Data provider is not set");
        }
    }

    /**
     * Gets the component associated with a specific item.
     *
     * @param item
     *            the item to get the component for
     * @return the component for the item, or null if the item is not found
     */
    @Nullable
    public V getComponentFor(T item) {
        if (componentProvider == null) {
            throw new IllegalStateException("Component provider is not set");
        }
        if (dataProvider != null && dataProvider.getItems().contains(item)) {
            return componentProvider.apply(item);
        }
        return null;
    }

    private Grid<T> createGrid(ValueProvider<T, V> provider) {
        var newGrid = new Grid<T>();
        newGrid.setRowHeight(40);
        newGrid.addComponentColumn(provider);
        newGrid.setHeaderRowHeight(1);
        newGrid.setSizeFull();
        newGrid.setSelectionMode(SelectionMode.NONE);
        newGrid.addStyleNames(VaadinCreateTheme.ADMINVIEW_CATEGORY_GRID,
                VaadinCreateTheme.GRID_NO_STRIPES,
                VaadinCreateTheme.GRID_NO_BORDERS,
                VaadinCreateTheme.GRID_NO_CELL_FOCUS);
        newGrid.setHeightMode(HeightMode.ROW);
        AttributeExtension.of(newGrid).setAttribute("tabindex", "-1");
        return newGrid;
    }

}
