package org.vaadin.tatu.vaadincreate.uiunittest.testers;

import java.lang.reflect.InvocationTargetException;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.vaadin.tatu.vaadincreate.uiunittest.Tester;

import com.vaadin.data.ValueProvider;
import com.vaadin.shared.MouseEventDetails;
import com.vaadin.shared.MouseEventDetails.MouseButton;
import com.vaadin.ui.Grid;
import com.vaadin.ui.components.grid.MultiSelectionModel;
import com.vaadin.ui.components.grid.MultiSelectionModelImpl;
import com.vaadin.ui.components.grid.SingleSelectionModel;
import com.vaadin.ui.components.grid.SingleSelectionModelImpl;

public class GridTester<T> extends Tester<Grid<T>> {

    public GridTester(Grid<T> grid) {
        super(grid);
    }

    /**
     * Return the content of the cell. If ComponentRenderer was used it is the
     * Component produced by the renderer otherwise it is the value.
     *
     * @param column
     *            Column index
     * @param row
     *            The row index
     * @return Cell content
     */
    public Object cell(int column, int row) {
        assert (column > -1 && column < getComponent().getColumns()
                .size()) : "Column out of bounds";
        assert (row > -1 && row < size()) : "Row out of bounds";
        var cat = (T) item(row);
        var vp = (ValueProvider<T, ?>) getComponent().getColumns().get(column)
                .getValueProvider();
        return vp.apply(cat);
    }

    /**
     * Return data item of the row.
     *
     * @param row
     *            Row index
     * @return The item
     */
    public T item(int row) {
        assert (row > -1 && row < size()) : "Row out of bounds";
        return getComponent().getDataCommunicator().fetchItemsWithRange(row, 1)
                .get(0);
    }

    /**
     * Return the total amount of rows as reported by DataProvider.
     *
     * @return int value
     */
    public int size() {
        return getComponent().getDataCommunicator().getDataProviderSize();
    }

    /**
     * Simulate click in given cell. Will trigger ItemClick event as a user. If
     * selection mode is Single or Multi, selection is updated accordingly.
     *
     * @param column
     *            Column index
     * @param row
     *            Row index
     */
    public void click(int column, int row) {
        assert (!getComponent().isReadOnly() && getComponent()
                .isEnabled()) : "Can't interact to readOnly or disabled Grid";
        assert (column > -1 && column < getComponent().getColumns()
                .size()) : "Column out of bounds";
        assert (row > -1 && row < size()) : "Row out of bounds";
        T i = item(row);
        var details = new MouseEventDetails();
        details.setButton(MouseButton.LEFT);
        var event = new Grid.ItemClick<T>(getComponent(),
                getComponent().getColumns().get(column), i, details, row);
        fireSimulatedEvent(event);
        if (getComponent().getSelectionModel() instanceof MultiSelectionModel) {
            if (getComponent().getSelectedItems().contains(i)) {
            } else {
                select(Set.of(i));
            }
        } else if (getComponent()
                .getSelectionModel() instanceof SingleSelectionModel) {
            if (getComponent().getSelectedItems().contains(i)) {
                deselect(i);
            } else {
                select(i);
            }
        }
    }

    /**
     * Select items as a user. Grid needs to be multiselect.
     *
     * @param items
     *            Items to be added into selection.
     */
    public void select(Set<T> items) {
        assert (!getComponent().isReadOnly() && getComponent()
                .isEnabled()) : "Can't interact to readOnly or disabled Grid";
        assert (getComponent()
                .getSelectionModel() instanceof MultiSelectionModel) : "Grid is not multiselect";
        assert (items != null) : "Items can't be null";
        Set<T> copy = getComponent().getSelectedItems().stream()
                .map(Objects::requireNonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        copy.addAll(items);
        var removed = new LinkedHashSet<>(getComponent().getSelectedItems());
        updateSelection(copy, removed);
    }

    /**
     * De-select items as a user. Grid needs to be multiselect.
     *
     * @param items
     *            Items to be removed from the selection.
     */
    public void deselect(Set<T> items) {
        assert (!getComponent().isReadOnly() && getComponent()
                .isEnabled()) : "Can't interact to readOnly or disabled Grid";
        assert (getComponent()
                .getSelectionModel() instanceof MultiSelectionModel) : "Grid is not multiselect";
        assert (items != null) : "Items can't be null";
        Set<T> copy = getComponent().getSelectedItems().stream()
                .map(Objects::requireNonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        copy.removeAll(items);
        var removed = new LinkedHashSet<>(getComponent().getSelectedItems());
        updateSelection(copy, removed);
    }

    private void updateSelection(Set<T> copy, LinkedHashSet<T> removed) {
        var model = (MultiSelectionModelImpl<T>) getComponent()
                .getSelectionModel();
        Class<?> clazz = model.getClass();
        try {
            var updateSelectionMethod = clazz.getDeclaredMethod(
                    "updateSelection", Set.class, Set.class, Boolean.TYPE);
            updateSelectionMethod.setAccessible(true);
            updateSelectionMethod.invoke(model, copy, removed, true);
        } catch (NoSuchMethodException | SecurityException
                | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    /**
     * Select given item. Grid needs to be singleselect.
     *
     * @param item
     *            Item to select, null to deselect existing.
     */
    public void select(T item) {
        assert (!getComponent().isReadOnly() && getComponent()
                .isEnabled()) : "Can't interact to readOnly or disabled Grid";
        assert (getComponent()
                .getSelectionModel() instanceof SingleSelectionModel) : "Grid is not singleselect";
        var key = getComponent().getDataCommunicator().getKeyMapper().key(item);
        var model = (SingleSelectionModelImpl<T>) getComponent()
                .getSelectionModel();
        Class<?> clazz = model.getClass();
        try {
            var setSelectedFromClientMethod = clazz
                    .getDeclaredMethod("setSelectedFromClient", String.class);
            setSelectedFromClientMethod.setAccessible(true);
            setSelectedFromClientMethod.invoke(model, key);
        } catch (NoSuchMethodException | SecurityException
                | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    /**
     * De-select the item. Grid needs to be singleselect.
     *
     * @param item
     *            Item to de-select.
     */
    public void deselect(T item) {
        item = null;
        select(item);
    }

    @Override
    protected Grid<T> getComponent() {
        return super.getComponent();
    }
}
