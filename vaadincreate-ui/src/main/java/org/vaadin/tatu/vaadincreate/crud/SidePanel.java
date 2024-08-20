package org.vaadin.tatu.vaadincreate.crud;

import org.vaadin.tatu.vaadincreate.VaadinCreateTheme;

import com.vaadin.ui.Component;
import com.vaadin.ui.Composite;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.JavaScript;

/**
 * Side panel layout with CSS animation sliding in / out.
 */
@SuppressWarnings("serial")
public class SidePanel extends Composite {
    CssLayout layout = new CssLayout();

    public SidePanel() {
        layout.setId("book-form");
        layout.addStyleNames(VaadinCreateTheme.BOOKFORM,
                VaadinCreateTheme.BOOKFORM_WRAPPER);

        setCompositionRoot(layout);
    }

    /**
     * Set side panel content.
     *
     * @param content Component
     */
    public void setContent(Component content) {
        layout.removeAllComponents();
        layout.addComponent(content);
    }

    @Override
    public void setVisible(boolean visible) {
        // This process is tricky. The element needs to be in DOM and not
        // having
        // 'display: none' in order to CSS animations to work. We will set
        // display none after a delay so that pressing 'tab' key will not
        // reveal
        // the form while set not visible.
        if (visible) {
            JavaScript.eval(
                    "document.getElementById('book-form').style.display='block';");
            getUI().runAfterRoundTrip(() -> layout.addStyleName(
                    VaadinCreateTheme.BOOKFORM_WRAPPER_VISIBLE));
        } else {
            layout.removeStyleName(
                    VaadinCreateTheme.BOOKFORM_WRAPPER_VISIBLE);
            if (isAttached()) {
                getUI().runAfterRoundTrip(() -> {
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    JavaScript.eval(
                            "document.getElementById('book-form').style.display='none';");
                });
            }
        }
    }
}

