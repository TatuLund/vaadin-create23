package org.vaadin.tatu.vaadincreate;

import org.junit.Assert;
import org.junit.Test;

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
        Assert.assertEquals("number", field.getAttribute("type"));
    }
}
