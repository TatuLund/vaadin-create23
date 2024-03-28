package org.vaadin.tatu.vaadincreate.uiunittest;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.mockito.Mockito;
import org.vaadin.tatu.vaadincreate.uiunittest.mocks.MockUI;
import org.vaadin.tatu.vaadincreate.uiunittest.mocks.MockVaadinService;
import org.vaadin.tatu.vaadincreate.uiunittest.mocks.MockVaadinSession;
import org.vaadin.tatu.vaadincreate.uiunittest.testers.AbstractFieldTester;
import org.vaadin.tatu.vaadincreate.uiunittest.testers.AbstractMultiSelectTester;
import org.vaadin.tatu.vaadincreate.uiunittest.testers.AbstractSingleSelectTester;
import org.vaadin.tatu.vaadincreate.uiunittest.testers.ButtonTester;
import org.vaadin.tatu.vaadincreate.uiunittest.testers.ComboBoxTester;
import org.vaadin.tatu.vaadincreate.uiunittest.testers.GridTester;
import org.vaadin.tatu.vaadincreate.uiunittest.testers.TabSheetTester;

import com.vaadin.server.AbstractClientConnector;
import com.vaadin.server.ServiceException;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinServletRequest;
import com.vaadin.server.VaadinServletResponse;
import com.vaadin.server.VaadinServletService;
import com.vaadin.server.VaadinSession;
import com.vaadin.ui.AbstractField;
import com.vaadin.ui.AbstractMultiSelect;
import com.vaadin.ui.AbstractSingleSelect;
import com.vaadin.ui.Button;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.Grid;
import com.vaadin.ui.HasComponents;
import com.vaadin.ui.Notification;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.UI;
import com.vaadin.ui.Window;

/**
 * Base class for unit testing complex Vaadin Components and UIs.
 */
public abstract class UIUnitTest {

    /**
     * Create mocked Vaadin environment with blank UI without Atmosphere
     * support. This is enough for common use cases testing standalone server
     * side components. Session is locked.
     *
     * @see tearDown()
     * @throws ServiceException
     * @return Plank mock UI instance
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

    /**
     * Create mocked Vaadin environment with given UI without Atmosphere
     * support. This is makes possible to test more complex UI logic. Session is
     * locked. UI and VaadinSession threadlocals are set.
     *
     * @see tearDown()
     * @param ui
     *            UI instance
     * @throws ServiceException
     */
    public void mockVaadin(UI ui) throws ServiceException {
        assert (ui != null) : "UI can't be null";
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

    /**
     * Do clean-up of the mocked Vaadin created with mockVaadin methods.
     *
     * @see mockVaadin(ui
     * @see mockVaadin(UI)
     */
    public void tearDown() {
        var session = VaadinSession.getCurrent();
        var ui = UI.getCurrent();
        ui.detach();
        ui.close();
        session.close();
        VaadinSession.setCurrent(null);
        UI.setCurrent(null);
    }

    /**
     * Navigate to a view
     *
     * @param <T>
     *            Target type
     * @param name
     *            The navigation path as a String, can include url parameters
     * @param clazz
     *            The class of the target view
     * @return
     */
    public <T> T navigate(String name, Class<T> clazz) {
        assert (name != null);
        assert (clazz != null);
        var nav = UI.getCurrent().getNavigator();
        assert (nav != null) : "Navigator does not exists";
        nav.navigateTo(name);
        var view = nav.getCurrentView();
        assert (view.getClass().equals(clazz));
        return (T) view;
    }

    /**
     * Find all components of given type recursively within the UI.
     * 
     * @see QueryResult
     *
     * @param <T>
     *            Component type
     * @param clazz
     *            Component class
     * @return QueryResult of components
     */
    public <T extends AbstractClientConnector> QueryResult<T> $(
            Class<T> clazz) {
        assert (clazz != null);
        assert (UI.getCurrent() != null) : "UI has not been setup";
        if (clazz.isAssignableFrom(Window.class)) {
            return new QueryResult<T>(
                    (Collection<T>) UI.getCurrent().getWindows());
        }
        if (clazz.equals(Notification.class)) {
            return new QueryResult<T>(
                    (Collection<T>) UI.getCurrent().getExtensions().stream()
                            .filter(ext -> ext instanceof Notification)
                            .collect(Collectors.toList()));
        }
        return $(UI.getCurrent(), clazz);
    }

    /**
     * Find all components of given type recursively within the given component
     * container.
     * 
     * @see QueryResult
     *
     * @param <T>
     *            Component type
     * @param container
     *            Component container to search
     * @param clazz
     *            Component class
     * @return QueryResult of components
     */
    public <T extends AbstractClientConnector> QueryResult<T> $(
            HasComponents container, Class<T> clazz) {
        assert (container != null && clazz != null);
        var iter = container.iterator();
        var result = new QueryResult<T>();
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

    /**
     * Perform operations with the component as a user. E.g. if the operation
     * fires an event as an side effect, it has isUserOriginated = true.
     *
     * @param component
     *            The component
     * @return Tester for operations
     */
    public ButtonTester test(Button component) {
        return new ButtonTester(component);
    }

    /**
     * Perform operations with the component as a user. E.g. if the operation
     * fires an event as an side effect, it has isUserOriginated = true.
     *
     * @param component
     *            The component
     * @return Tester for operations
     */
    public <T> ComboBoxTester<T> test(ComboBox<T> component) {
        return new ComboBoxTester<>(component);
    }

    /**
     * Perform operations with the component as a user. E.g. if the operation
     * fires an event as an side effect, it has isUserOriginated = true.
     *
     * @param component
     *            The component
     * @return Tester for operations
     */
    public TabSheetTester test(TabSheet component) {
        return new TabSheetTester(component);
    }

    /**
     * Perform operations with the component as a user. E.g. if the operation
     * fires an event as an side effect, it has isUserOriginated = true.
     *
     * @param component
     *            The component
     * @return Tester for operations
     */
    public <T> GridTester<T> test(Grid<T> component) {
        return new GridTester<>(component);
    }

    /**
     * Perform operations with the component as a user. E.g. if the operation
     * fires an event as an side effect, it has isUserOriginated = true.
     *
     * @param component
     *            The component
     * @return Tester for operations
     */
    public <T> AbstractFieldTester<T> test(AbstractField<T> component) {
        return new AbstractFieldTester<>(component);
    }

    /**
     * Perform operations with the component as a user. E.g. if the operation
     * fires an event as an side effect, it has isUserOriginated = true.
     *
     * @param component
     *            The component
     * @return Tester for operations
     */
    public <T> AbstractSingleSelectTester<T> test(
            AbstractSingleSelect<T> component) {
        return new AbstractSingleSelectTester<>(component);
    }

    /**
     * Perform operations with the component as a user. E.g. if the operation
     * fires an event as an side effect, it has isUserOriginated = true.
     *
     * @param component
     *            The component
     * @return Tester for operations
     */
    public <T> AbstractMultiSelectTester<T> test(
            AbstractMultiSelect<T> component) {
        return new AbstractMultiSelectTester<>(component);
    }

    /**
     * Utility mehtod that waits while condition is true. Unlocks the mocked
     * session and returns lock after wait ends. This is useful when waiting
     * background thread activity to complete and letting ui.access to happen.
     *
     * @see UI#access(Runnable)
     *
     * @param <T>
     *            Parameter type
     * @param param
     *            Parameter for the predicate
     * @param condition
     *            Boolean predicate, can be lambda expression
     * @param timeout
     *            Wait maximum seconds
     */
    public <T> void waitWhile(T param, Predicate<T> condition, int timeout) {
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
            } while (condition.test(param) && i < timeout);
        } finally {
            VaadinSession.getCurrent().lock();
        }
    }

    /**
     * Result type for component searches.
     *
     * @see $(Class)
     * @see $(HasComponents, Class)
     *
     * @param <T>
     *            Component type
     */
    public static class QueryResult<T extends AbstractClientConnector>
            extends ArrayList<T> {
        public QueryResult(Collection<T> list) {
            super(list);
        }

        public QueryResult() {
            super();
        }

        /**
         * Find the component by id using exact match. Returns the first
         * matching component by id within the search results assuming ids are
         * unique.
         *
         * @param id
         *            The id as String
         * @return Component instance, can be null
         */
        public T id(String id) {
            assert (isEmpty() ? first() instanceof Component
                    : true) : "Id matching can be done with Components only";
            var res = stream().filter(c -> ((Component) c).getId().equals(id))
                    .findFirst();
            if (res.isPresent()) {
                return res.get();
            }
            return null;
        }

        /**
         * Find components by matching style name, using partial matching.
         * Result can contain many components.
         *
         * @param styleName
         *            Style name as String
         * @return Result set of components
         */
        public QueryResult<T> styleName(String styleName) {
            assert (isEmpty() ? first() instanceof Component
                    : true) : "Stylename filtering can be done with Components only";
            return new QueryResult<>(stream().filter(
                    c -> ((Component) c).getStyleName().contains(styleName))
                    .collect(Collectors.toList()));
        }

        /**
         * Find components by matching caption, using partial matching. Result
         * can contain many components.
         *
         * @param caption
         *            Caption as String
         * @return Result set of components
         */
        public QueryResult<T> caption(String caption) {
            assert (isEmpty() ? first() instanceof Component
                    : true) : "Caption filtering can be done with Components only";
            return new QueryResult<>(stream()
                    .filter(c -> ((Component) c).getCaption().contains(caption))
                    .collect(Collectors.toList()));
        }

        /**
         * Return the first component in the list.
         *
         * @return Component, null if the list was empty.
         */
        public T first() {
            if (isEmpty()) {
                return null;
            }
            return get(0);
        }

        /**
         * Return the last component in the list.
         *
         * @return Component, null if the list was empty.
         */
        public T last() {
            if (isEmpty()) {
                return null;
            }
            return get(size() - 1);
        }

        /**
         * Return the only component in the list if it exists, otherwise fail
         * with assertion.
         *
         * @return Component.
         */
        public T single() {
            assert (size() == 1) : "There are more than one components";
            return get(0);
        }
    }
}
