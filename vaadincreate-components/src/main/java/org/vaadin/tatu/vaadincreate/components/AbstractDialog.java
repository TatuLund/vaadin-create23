package org.vaadin.tatu.vaadincreate.components;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import com.vaadin.shared.Registration;
import com.vaadin.ui.AbstractComponent;
import com.vaadin.ui.UI;
import com.vaadin.ui.Window;

/**
 * AbstractDialog is a base class for modal dialog windows in the application.
 * It provides common functionality such as centering the dialog and handling
 * browser resize events to keep the dialog centered.
 */
@NullMarked
@SuppressWarnings({ "serial", "java:S2160" })
public abstract class AbstractDialog extends AbstractComponent {

    protected Window window = new Window();

    @Nullable
    private Registration resizeRegistration;

    /**
     * Constructs a new AbstractDialog with default settings. The dialog is
     * modal, not resizable, and centered on the screen. It also registers a
     * listener to re-center the dialog when the browser window is resized.
     * <p>
     * Subclasses can customize the dialog by modifying the {@code window} field
     * after calling the superclass constructor. The dialog is not opened by
     * default; call the {@code open()} method to display it.
     * <p>
     * The dialog will automatically clean up the resize listener when it is
     * closed to prevent memory leaks.
     */
    protected AbstractDialog() {
        window.setModal(true);
        window.setResizable(false);
        window.setWidth("50%");
        window.setHeight("50%");
        window.setDraggable(false);
        window.addCloseListener(closeEvent -> {
            if (resizeRegistration != null) {
                resizeRegistration.remove();
                resizeRegistration = null;
            }
        });
    }

    /**
     * Open the dialog
     */
    public void open() {
        var ui = UI.getCurrent();
        ui.addWindow(window);
        window.center();
        resizeRegistration = ui.getPage()
                .addBrowserWindowResizeListener(resizeEvent -> window.center());
    }
}
