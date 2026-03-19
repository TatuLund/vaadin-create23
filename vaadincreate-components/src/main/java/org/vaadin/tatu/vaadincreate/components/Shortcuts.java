package org.vaadin.tatu.vaadincreate.components;

import com.vaadin.ui.JavaScript;

public class Shortcuts {

    private Shortcuts() {
        // Utility class
    }

    /**
     * Sets up a listener on given layout to listen for Escape key presses and
     * click the button matching the given selector when Escape is pressed.
     *
     * @param layoutSelector
     *            the CSS selector for the layout to listen on
     * @param buttonSelector
     *            the CSS selector for the button to click when Escape is
     *            pressed
     */
    public static void setEscapeShortcut(String layoutSelector,
            String buttonSelector) {
        String script = """
                const dialog = document.querySelector('%s');
                if (dialog) {
                    const button = dialog.querySelector('%s');
                    if (button) {
                        dialog.addEventListener('keydown', (event) => {
                            if (event.key === 'Escape') {
                                button.click();
                            }
                        });
                    }
                }
                """.formatted(layoutSelector, buttonSelector);
        JavaScript.eval(script);
    }
}