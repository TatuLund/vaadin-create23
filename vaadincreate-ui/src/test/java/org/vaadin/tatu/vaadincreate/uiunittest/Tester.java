package org.vaadin.tatu.vaadincreate.uiunittest;

import java.lang.reflect.InvocationTargetException;
import java.util.EventObject;

import com.vaadin.server.AbstractClientConnector;
import com.vaadin.ui.Component;

public abstract class Tester<T extends Component> {

    private Component component;

    public Tester(T component) {
        this.component = component;
    }

    /**
     * Utility method to fire a fabricated event.
     * 
     * @param event The event to be fired
     */
    protected void fireSimulatedEvent(EventObject event) {
        Class<?> clazz = component.getClass();
        while (!clazz.equals(AbstractClientConnector.class)) {
            clazz = clazz.getSuperclass();
        }
        try {
            var fireEventMethod = clazz.getDeclaredMethod("fireEvent",
                    EventObject.class);
            fireEventMethod.setAccessible(true);
            fireEventMethod.invoke(component, event);
        } catch (NoSuchMethodException | SecurityException
                | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

}
