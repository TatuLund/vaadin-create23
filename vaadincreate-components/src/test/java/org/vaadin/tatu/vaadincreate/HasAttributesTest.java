package org.vaadin.tatu.vaadincreate;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.vaadin.tatu.vaadincreate.AttributeExtension.HasAttributes;

import com.vaadin.ui.CssLayout;

public class HasAttributesTest {

    @Test
    public void testSetAriaLabel() {
        TestComponent component = new TestComponent();
        component.setAriaLabel("Test Label");
        assertEquals("Test Label", component.getAttribute("aria-label"));
    }

    @Test
    public void testSetRole() {
        TestComponent component = new TestComponent();
        component.setRole("button");
        assertEquals("button", component.getAttribute("role"));
    }

    @Test
    public void testSetAndRemoveAttribute() {
        TestComponent component = new TestComponent();
        component.setAttribute("custom-attribute", "value");
        assertEquals("value", component.getAttribute("custom-attribute"));

        component.removeAttribute("custom-attribute");
        assertEquals(null, component.getAttribute("custom-attribute"));
    }

    public static class TestComponent extends CssLayout
            implements HasAttributes<TestComponent> {
    }
}
