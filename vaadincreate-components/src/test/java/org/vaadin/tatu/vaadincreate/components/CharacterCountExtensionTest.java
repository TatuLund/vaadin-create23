package org.vaadin.tatu.vaadincreate.components;

import org.junit.Test;

import com.vaadin.ui.ComboBox;

public class CharacterCountExtensionTest {

    @Test(expected = IllegalArgumentException.class)
    public void wrongComponentThrows() {
        ComboBox<String> comboBox = new ComboBox<>();
        CharacterCountExtension.extend(comboBox);
    }
}
