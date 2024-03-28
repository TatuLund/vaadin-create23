package org.vaadin.tatu.vaadincreate.uiunittest.testers;

import java.lang.reflect.InvocationTargetException;

import org.vaadin.tatu.vaadincreate.uiunittest.HasValue;
import org.vaadin.tatu.vaadincreate.uiunittest.Tester;

import com.vaadin.ui.AbstractSingleSelect;

public class AbstractSingleSelectTester<T>
        extends Tester<AbstractSingleSelect<T>> implements HasValue<T> {

    public AbstractSingleSelectTester(AbstractSingleSelect<T> field) {
        super(field);
    }

    @Override
    public void setValue(T value) {
        assert (!getComponent().isReadOnly() && getComponent()
                .isEnabled()) : "Can't set value to readOnly or disabled field";
        Class<?> clazz = getComponent().getClass();
        while (!clazz.equals(AbstractSingleSelect.class)) {
            clazz = clazz.getSuperclass();
        }
        try {
            var setSelectedItemMethod = clazz.getDeclaredMethod(
                    "setSelectedItem", Object.class, Boolean.TYPE);
            setSelectedItemMethod.setAccessible(true);
            setSelectedItemMethod.invoke(getComponent(), value, true);
        } catch (NoSuchMethodException | SecurityException
                | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected AbstractSingleSelect<T> getComponent() {
        return super.getComponent();
    }
}
