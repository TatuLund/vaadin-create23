package org.vaadin.tatu.vaadincreate.uiunittest;

import java.lang.reflect.InvocationTargetException;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.vaadin.ui.AbstractMultiSelect;

public class AbstractMultiSelectTester<T>
        extends Tester<AbstractMultiSelect<T>> {

    private AbstractMultiSelect<T> field;

    public AbstractMultiSelectTester(AbstractMultiSelect<T> field) {
        super(field);
        this.field = field;
    }

    public void setValue(Set<T> value) {
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
