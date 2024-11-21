package org.vaadin.tatu.vaadincreate;

import org.vaadin.tatu.vaadincreate.shared.ResetButtonForTextFieldState;

import com.vaadin.server.AbstractExtension;
import com.vaadin.ui.TextField;

/**
 * An extension adding a button in a text field for clearing the field. Only
 * shown when the text field is non-empty.
 * 
 * @see <a href="https://vaadin.com/blog/-/blogs/2656782">Extending components
 *      in Vaadin 7</a>
 */
@SuppressWarnings("serial")
public class ResetButtonForTextField extends AbstractExtension {

    protected ResetButtonForTextField(TextField field) {
        // Non-public constructor to discourage direct instantiation
        extend(field);
    }

    /**
     * Creates a new instance of ResetButtonForTextField for the given
     * TextField.
     *
     * @param field
     *            the TextField to associate with the reset button
     * @return a new instance of ResetButtonForTextField
     */
    public static ResetButtonForTextField of(TextField field) {
        return new ResetButtonForTextField(field);
    }

    /**
     * Sets the accessibility label for the reset button.
     *
     * @param buttonLabel
     *            the new label for the reset button
     */
    public void setButtonLabel(String buttonLabel) {
        getState().buttonLabel = buttonLabel;
    }

    @Override
    public ResetButtonForTextFieldState getState() {
        return (ResetButtonForTextFieldState) super.getState();
    }

}
