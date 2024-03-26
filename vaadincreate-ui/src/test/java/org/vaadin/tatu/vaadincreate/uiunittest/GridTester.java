package org.vaadin.tatu.vaadincreate.uiunittest;

import com.vaadin.data.ValueProvider;
import com.vaadin.ui.Grid;

public class GridTester<T> extends Tester<Grid<T>> {
    private Grid<T> grid;

    public GridTester(Grid<T> grid) {
        this.grid = grid;
    }

    public Object cell(int column, int row) {
        assert (grid != null);
        assert (column > -1 && column < grid.getColumns().size());
        assert (row > -1 && row < size());
        var cat = (T) item(row);
        var vp = (ValueProvider<T, ?>) grid.getColumns().get(column)
                .getValueProvider();
        return vp.apply(cat);
    }

    public T item(int index) {
        assert (grid != null);
        return grid.getDataCommunicator().fetchItemsWithRange(index, 1).get(0);
    }

    public int size() {
        assert (grid != null);
        return grid.getDataCommunicator().getDataProviderSize();
    }

}
