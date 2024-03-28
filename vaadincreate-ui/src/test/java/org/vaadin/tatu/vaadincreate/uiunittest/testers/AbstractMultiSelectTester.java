package org.vaadin.tatu.vaadincreate.uiunittest.testers;

import java.lang.reflect.InvocationTargetException;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.vaadin.tatu.vaadincreate.uiunittest.HasValue;
import org.vaadin.tatu.vaadincreate.uiunittest.Tester;

import com.vaadin.ui.AbstractMultiSelect;

public class AbstractMultiSelectTester<T> extends Tester<AbstractMultiSelect<T>>
        implements HasValue<Set<T>> {

    private AbstractMultiSelect<T> field;

    public AbstractMultiSelectTester(AbstractMultiSelect<T> field) {
        super(field);
        this.field = field;
    }

    @Override
    public void setValue(Set<T> value) {
        assert (!field.isReadOnly() && field
                .isEnabled()) : "Can't set value to readOnly or disabled field";
        Set<T> copy = value.stream().map(Objects::requireNonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Class<?> clazz = field.getClass();
        while (!clazz.equals(AbstractMultiSelect.class)) {
            clazz = clazz.getSuperclass();
        }
        try {
            var updateSelectionMethod = clazz.getDeclaredMethod(
                    "updateSelection", Set.class, Set.class, Boolean.TYPE);
            updateSelectionMethod.setAccessible(true);
            updateSelectionMethod.invoke(field, copy,
                    new LinkedHashSet<>(field.getSelectedItems()), true);
        } catch (NoSuchMethodException | SecurityException
                | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException e) {
            e.printStackTrace();
        }
    }
}
