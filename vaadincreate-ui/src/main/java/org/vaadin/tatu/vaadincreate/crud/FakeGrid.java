package org.vaadin.tatu.vaadincreate.crud;

import org.jspecify.annotations.NullMarked;
import org.vaadin.tatu.vaadincreate.VaadinCreateTheme;
import org.vaadin.tatu.vaadincreate.AttributeExtension.HasAttributes;
import org.vaadin.tatu.vaadincreate.i18n.HasI18N;
import org.vaadin.tatu.vaadincreate.i18n.I18n;

import com.vaadin.ui.Alignment;
import com.vaadin.ui.Composite;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

/**
 * The {@code FakeGrid} class represents a fake grid component. It is used to
 * display a loading spinner while the actual grid data is being fetched.
 *
 * <p>
 * Usage example:
 *
 * <pre>
 * {@code
 * FakeGrid fakeGrid = new FakeGrid();
 * }
 * </pre>
 *
 * <p>
 * Layout properties:
 * <ul>
 * <li>Main layout is set to full height, with no margin or spacing.</li>
 * <li>Spinner wrapper layout is set to full height, with no margin or spacing,
 * and the spinner is centered.</li>
 * <li>Fake header and spinner wrapper are added to the main layout, with the
 * spinner wrapper expanding to fill available space.</li>
 * </ul>
 */
@NullMarked
@SuppressWarnings("serial")
public class FakeGrid extends Composite implements HasI18N {

    /**
     * This class represents a fake grid component. It is used to display a
     * loading spinner while the actual grid data is being fetched.
     */
    public FakeGrid() {
        VerticalLayout layout = new VerticalLayout();
        setCompositionRoot(layout);
        layout.setId("fake-grid");
        layout.addStyleName(VaadinCreateTheme.FAKEGRID);
        layout.setHeightFull();
        layout.setMargin(false);
        layout.setSpacing(false);
        var spinnerWrapper = new VerticalLayout();
        spinnerWrapper.addStyleName(VaadinCreateTheme.FAKEGRID_SPINNERWRAPPER);
        spinnerWrapper = new VerticalLayout();
        spinnerWrapper.setHeightFull();
        spinnerWrapper.setMargin(false);
        spinnerWrapper.setSpacing(false);
        var fakeHeader = new CssLayout();
        fakeHeader.addStyleName(VaadinCreateTheme.FAKEGRID_HEADER);
        var spinner = new Spinner();
        spinnerWrapper.addComponent(spinner);
        spinnerWrapper.setComponentAlignment(spinner, Alignment.MIDDLE_CENTER);
        layout.addComponents(fakeHeader, spinnerWrapper);
        layout.setExpandRatio(spinnerWrapper, 1);
    }

    @SuppressWarnings("java:S2160")
    static class Spinner extends Label implements HasAttributes, HasI18N {

        /**
         * Creates a new spinner label with the loading style.
         */
        public Spinner() {
            super();
            addStyleName(ValoTheme.LABEL_SPINNER);
            // Set ARIA attributes for the spinner to make it accessible
            setAttribute("aria-label", getTranslation(I18n.Books.LOADING));
            setAttribute("aria-live", "assertive");
            setAttribute("role", "alert");
        }
    }
}
