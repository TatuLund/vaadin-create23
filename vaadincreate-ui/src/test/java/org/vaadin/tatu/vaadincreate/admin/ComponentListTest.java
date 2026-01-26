package org.vaadin.tatu.vaadincreate.admin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

public class ComponentListTest {

    @Test
    public void removeItemThrowsExceptionWhenDataProviderNotSet() {
        var componentList = new ComponentList<String, CategoryForm>(
                item -> null);
        try {
            componentList.removeItem("Test Item");
            fail("Expected IllegalStateException was not thrown");
        } catch (IllegalStateException e) {
            assertEquals("Data provider is not set", e.getMessage());
        }
    }

    @Test
    public void addItemThrowsExceptionWhenDataProviderNotSet() {
        var componentList = new ComponentList<String, CategoryForm>(
                item -> null);
        try {
            componentList.addItem("Test Item");
            fail("Expected IllegalStateException was not thrown");
        } catch (IllegalStateException e) {
            assertEquals("Data provider is not set", e.getMessage());
        }
    }

    @Test
    public void settingEmptyCollectionSetsOneRowHeight() {
        var componentList = new ComponentList<String, CategoryForm>(
                item -> null);
        componentList.setItems(java.util.Collections.emptyList());
        assertEquals(1d, componentList.grid.getHeightByRows(), 0.0);
    }
}
