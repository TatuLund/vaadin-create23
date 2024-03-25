package org.vaadin.tatu.vaadincreate.uiunittest;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.mockito.Mockito;

import com.vaadin.data.ValueProvider;
import com.vaadin.server.ServiceException;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinServletRequest;
import com.vaadin.server.VaadinServletResponse;
import com.vaadin.server.VaadinServletService;
import com.vaadin.server.VaadinSession;
import com.vaadin.ui.Component;
import com.vaadin.ui.ComponentContainer;
import com.vaadin.ui.Grid;
import com.vaadin.ui.HasComponents;
import com.vaadin.ui.UI;
import com.vaadin.ui.Window;

/**
 * Base class for unit testing complex Vaadin components.
 */
public abstract class UIUnitTest {

    /**
     * Create mocked Vaadin environment without Atmosphere support. This is
     * enough for common use cases. In Vaadin 23/24 version of the TestBench
     * there is more complete Vaadin environment mock together with component
     * helpers. Will set UI and Session thread locals.
     *
     * @throws ServiceException
     */
    public UI mockVaadin() throws ServiceException {
        var service = new MockVaadinService();
        var session = new MockVaadinSession(service);
        session.lock();
        VaadinSession.setCurrent(session);
        var ui = new MockUI(session);
        UI.setCurrent(ui);
        var request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getParameter("v-loc")).thenReturn("");
        Mockito.when(request.getParameter("v-cw")).thenReturn("1280");
        Mockito.when(request.getParameter("v-ch")).thenReturn("800");
        Mockito.when(request.getParameter("v-wn")).thenReturn("window");
        var vaadinRequest = new VaadinServletRequest(request,
                (VaadinServletService) session.getService());
        var response = Mockito.mock(HttpServletResponse.class);
        service.setCurrentInstances(vaadinRequest,
                new VaadinServletResponse(response, service));
        ui.getPage().init(vaadinRequest);
        return ui;
    }

    public void mockVaadin(UI ui) throws ServiceException {
        var service = new MockVaadinService();
        var session = new MockVaadinSession(service);
        session.lock();
        VaadinSession.setCurrent(session);
        ui.setSession(session);
        UI.setCurrent(ui);
        var request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getParameter("v-loc")).thenReturn("");
        Mockito.when(request.getParameter("v-cw")).thenReturn("1280");
        Mockito.when(request.getParameter("v-ch")).thenReturn("800");
        Mockito.when(request.getParameter("v-wn")).thenReturn("window");
        var vaadinRequest = new VaadinServletRequest(request,
                (VaadinServletService) session.getService());
        var response = Mockito.mock(HttpServletResponse.class);
        service.setCurrentInstances(vaadinRequest,
                new VaadinServletResponse(response, service));
        ui.getPage().init(vaadinRequest);
        Method initMethod;
        var clazz = ui.getClass();
        try {
            initMethod = clazz.getDeclaredMethod("init", VaadinRequest.class);
            initMethod.setAccessible(true);
            initMethod.invoke(ui, vaadinRequest);
        } catch (NoSuchMethodException | SecurityException
                | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException e) {
            System.out.println("Cannot call UI.init(VaadinRequest)");
            e.printStackTrace();
        }
    }

    public void tearDown() {
        var session = VaadinSession.getCurrent();
        var ui = UI.getCurrent();
        ui.detach();
        ui.close();
        session.close();
    }

    public Component navigate(String name) {
        assert (name != null);
        var nav = UI.getCurrent().getNavigator();
        nav.navigateTo(name);
        return (Component) nav.getCurrentView();
    }

    public <T extends Component> Result<T> $(Class<T> clazz) {
        assert (clazz != null);
        if (clazz.equals(Window.class)) {
            return new Result<T>((Collection<T>) UI.getCurrent().getWindows());
        }
        return $(UI.getCurrent(), clazz);
    }

    public <T extends Component> Result<T> $(HasComponents container,
            Class<T> clazz) {
        assert (container != null && clazz != null);
        var iter = container.iterator();
        var result = new Result<T>();
        while (iter.hasNext()) {
            var component = iter.next();
            if (component.getClass().equals(clazz)) {
                result.add((T) component);
            }
            if (component instanceof HasComponents) {
                result.addAll($((HasComponents) component, clazz));
            }
        }
        return result;
    }

//    public Component $(String id) {
//        assert (id != null);
//        return $(UI.getCurrent(), id);
//    }
//
//    public Component $(HasComponents container, String id) {
//        assert (id != null);
//        var iter = container.iterator();
//        while (iter.hasNext()) {
//            var component = iter.next();
//            if (component.getId() != null && component.getId().equals(id)) {
//                return component;
//            }
//            if (component instanceof HasComponents) {
//                return $((HasComponents) component, id);
//            }
//        }
//        return null;
//    }

    public <T> Object getGridCell(Grid<T> grid, int column, int row) {
        assert (grid != null);
        assert (column > -1 && column < grid.getColumns().size());
        assert (row > -1 && row < getGridSize(grid));
        var cat = (T) getGridItem(grid, row);
        var vp = (ValueProvider<T, ?>) grid.getColumns().get(column)
                .getValueProvider();
        return vp.apply(cat);
    }

    public <T> T getGridItem(Grid<T> grid, int index) {
        assert (grid != null);
        return grid.getDataCommunicator().fetchItemsWithRange(index, 1).get(0);
    }

    public <T> int getGridSize(Grid<T> grid) {
        assert (grid != null);
        return grid.getDataCommunicator().getDataProviderSize();
    }

    public <T> void waitWhile(T param, Predicate<T> condition) {
        assert (param != null);
        assert (condition != null);
        assert (VaadinSession.getCurrent().hasLock());
        VaadinSession.getCurrent().unlock();
        try {
            int i = 0;
            do {
                try {
                    Thread.sleep(1000);
                    i++;
                } catch (InterruptedException e) {
                }
            } while (condition.test(param) && i < 10);
        } finally {
            VaadinSession.getCurrent().lock();
        }
    }

    public static class Result<T extends Component> extends ArrayList<T> {
        public Result(Collection<T> list) {
            super(list);
        }

        public Result() {
            super();
        }

        public T id(String id) {
            var res = stream().filter(c -> c.getId().equals(id)).findFirst();
            if (res.isPresent()) {
                return res.get();
            }
            return null;
        }

        public Result<T> styleName(String styleName) {
            return new Result<>(
                    stream().filter(c -> c.getStyleName().contains(styleName))
                            .collect(Collectors.toList()));
        }

        public T first() {
            return get(0);
        }

        public T last() {
            return get(size() - 1);
        }
    }
}
