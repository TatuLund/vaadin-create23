package org.vaadin.tatu.vaadincreate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import com.vaadin.testbench.elements.ButtonElement;
import com.vaadin.testbench.elements.TextFieldElement;

public class AttributeExtensionIT extends AbstractComponentTest {

    @Override
    public void setup() throws Exception {
        super.setup();
        open("#!" + AttributeExtensionView.NAME);
    }

    @Test
    public void typeIsNumber() {
        var field = $(TextFieldElement.class).first();
        assertEquals("number", field.getAttribute("type"));
        assertEquals("custom", field.getAttribute("custom"));
        assertEquals("sticky", field.getAttribute("special"));
    }

    @Test
    public void removeAttribute() {
        $(ButtonElement.class).first().click();
        var field = $(TextFieldElement.class).first();
        assertEquals("text", field.getAttribute("type"));
        assertNull(field.getAttribute("custom"));
        assertEquals("sticky", field.getAttribute("special"));
    }
}
