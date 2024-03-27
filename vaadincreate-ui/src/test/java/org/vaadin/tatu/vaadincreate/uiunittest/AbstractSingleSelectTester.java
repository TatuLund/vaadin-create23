package org.vaadin.tatu.vaadincreate.uiunittest;

import java.lang.reflect.InvocationTargetException;

import com.vaadin.ui.AbstractSingleSelect;

public class AbstractSingleSelectTester<T>
        extends Tester<AbstractSingleSelect<T>> implements HasValue<T> {

    private AbstractSingleSelect<T> field;

    public AbstractSingleSelectTester(AbstractSingleSelect<T> field) {
        super(field);
        this.field = field;
    }

    @Override
    public void setValue(T value) {
        Class<?> clazz = field.getClass();
        while (!clazz.equals(AbstractSingleSelect.class)) {
            clazz = clazz.getSuperclass();
        }
        try {
            var setSelectedItemMethod = clazz.getDeclaredMethod(
                    "setSelectedItem", Object.class, Boolean.TYPE);
            setSelectedItemMethod.setAccessible(true);
            setSelectedItemMethod.invoke(field, value, true);
        } catch (NoSuchMethodException | SecurityException
                | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException e) {
            e.printStackTrace();
        }
    }
}
