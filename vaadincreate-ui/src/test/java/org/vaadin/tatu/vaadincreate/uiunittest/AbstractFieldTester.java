package org.vaadin.tatu.vaadincreate.uiunittest;

import java.lang.reflect.InvocationTargetException;

import com.vaadin.ui.AbstractField;

public class AbstractFieldTester<T> extends Tester<AbstractField<T>> implements HasValue<T> {

    private AbstractField<T> field;

    public AbstractFieldTester(AbstractField<T> field) {
        super(field);
        this.field = field;
    }

    @Override
    public void setValue(T value) {
        Class<?> clazz = field.getClass();
        while (!clazz.equals(AbstractField.class)) {
            clazz = clazz.getSuperclass();
        }
        try {
            var setValueMethod = clazz.getDeclaredMethod("setValue",
                    Object.class, Boolean.TYPE);
            setValueMethod.setAccessible(true);
            setValueMethod.invoke(field, value, true);
        } catch (NoSuchMethodException | SecurityException
                | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException e) {
            e.printStackTrace();
        }
    }
}
