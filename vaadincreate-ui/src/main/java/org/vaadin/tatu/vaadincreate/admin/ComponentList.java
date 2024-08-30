package org.vaadin.tatu.vaadincreate.admin;

import java.util.Collection;

import org.vaadin.tatu.vaadincreate.VaadinCreateTheme;

import com.vaadin.data.ValueProvider;
import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.shared.ui.grid.HeightMode;
import com.vaadin.ui.Component;
import com.vaadin.ui.Composite;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Grid.SelectionMode;

/**
 * A list of components generated from a collection of items. Components are
 * shown as scrollable list.
 */
@SuppressWarnings({ "serial", "java:S2160" })
public class ComponentList<T, V extends Component> extends Composite {
    private Grid<T> grid;
    private ListDataProvider<T> dataProvider;

    /**
     * Creates a new instance of ComponentList.
     *
     * @param provider
     *            ValueProvider to create components from items.
     */
    public ComponentList(ValueProvider<T, V> provider) {
        createCategoryListing(provider);
        setCompositionRoot(grid);
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
        dataProvider.getItems().remove(removed);
        dataProvider.getItems().add(added);
        dataProvider.refreshAll();
    }

    public void removeItem(T item) {
        dataProvider.getItems().remove(item);
        dataProvider.refreshAll();
        grid.setHeightByRows(dataProvider.getItems().size());
    }

    /**
     * Sets the items to be shown in the list. And generates components from the
     * items.
     *
     * @param items
     *            Collection of items
     */
    public void setItems(Collection<T> items) {
        dataProvider = new ListDataProvider<>(items);
        grid.setDataProvider(dataProvider);
        grid.setHeightByRows(items.size());
    }

    /**
     * Adds an item to the list and generates a component from the item.
     *
     * @param item
     *            item to be added
     */
    public void addItem(T item) {
        dataProvider.getItems().add(item);
        dataProvider.refreshAll();
        grid.setHeightByRows(dataProvider.getItems().size());
        grid.scrollToEnd();
    }

    private void createCategoryListing(ValueProvider<T, V> provider) {
        grid = new Grid<>();
        grid.setRowHeight(40);
        grid.addComponentColumn(provider);
        grid.setHeaderRowHeight(1);
        grid.setSizeFull();
        grid.setSelectionMode(SelectionMode.NONE);
        grid.addStyleNames(VaadinCreateTheme.ADMINVIEW_CATEGORY_GRID,
                VaadinCreateTheme.GRID_NO_STRIPES,
                VaadinCreateTheme.GRID_NO_BORDERS,
                VaadinCreateTheme.GRID_NO_CELL_FOCUS);
        grid.setHeightMode(HeightMode.ROW);
    }

}
