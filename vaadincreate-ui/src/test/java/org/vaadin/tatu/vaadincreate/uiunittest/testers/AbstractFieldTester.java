package org.vaadin.tatu.vaadincreate.uiunittest.testers;

import java.lang.reflect.InvocationTargetException;

import org.vaadin.tatu.vaadincreate.uiunittest.HasValue;
import org.vaadin.tatu.vaadincreate.uiunittest.Tester;

import com.vaadin.ui.AbstractField;

public class AbstractFieldTester<T> extends Tester<AbstractField<T>>
        implements HasValue<T> {

    public AbstractFieldTester(AbstractField<T> field) {
        super(field);
    }

    @Override
    public void setValue(T value) {
        assert (!getComponent().isReadOnly() && getComponent()
                .isEnabled()) : "Can't set value to readOnly or disabled field";
        Class<?> clazz = getComponent().getClass();
        while (!clazz.equals(AbstractField.class)) {
            clazz = clazz.getSuperclass();
        }
        try {
            var setValueMethod = clazz.getDeclaredMethod("setValue",
                    Object.class, Boolean.TYPE);
            setValueMethod.setAccessible(true);
            setValueMethod.invoke(getComponent(), value, true);
        } catch (NoSuchMethodException | SecurityException
                | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected AbstractField<T> getComponent() {
        return super.getComponent();
    }
}
