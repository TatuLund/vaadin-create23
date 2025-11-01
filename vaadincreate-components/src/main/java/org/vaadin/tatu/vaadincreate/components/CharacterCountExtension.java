package org.vaadin.tatu.vaadincreate.components;

import org.jspecify.annotations.NullMarked;

import com.vaadin.server.AbstractClientConnector;
import com.vaadin.server.AbstractExtension;
import com.vaadin.ui.Component;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.TextField;

@NullMarked
@SuppressWarnings("serial")
public class CharacterCountExtension extends AbstractExtension {

    /**
     * Add character count to the field.
     * 
     * @param field
     *            TextField or TextArea
     */
    public static void extend(Component field) {
        if (field instanceof TextField || field instanceof TextArea) {
            new CharacterCountExtension()
                    .extend((AbstractClientConnector) field);
        } else {
            throw new IllegalArgumentException(
                    "CharacterCountExtension works only with TextField and TextArea");
        }
    }
}
