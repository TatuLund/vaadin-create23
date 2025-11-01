package org.vaadin.tatu.vaadincreate.components;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;

import com.vaadin.testbench.elements.PasswordFieldElement;

public class CapsLockWarningIT extends AbstractComponentTest {

    @Override
    public void setup() throws Exception {
        super.setup();
        open("#!" + CapsLockWarningView.NAME);
    }

    @Test
    public void capsLockMessage() {
        var field = $(PasswordFieldElement.class).first();
        field.focus();
        // Selenium does not support Caps Lock in Keys, hence we use JavaScript
        executeScript(
                "arguments[0].dispatchEvent(new KeyboardEvent('keydown', {key:'A', modifierCapsLock: true}));",
                field);
        var message = findElement(By.className("capslock-message"));
        Assert.assertEquals("Caps Lock", message.getText());
    }

}
