package org.vaadin.tatu.vaadincreate.crud;

import org.vaadin.tatu.vaadincreate.VaadinCreateTheme;

import com.vaadin.ui.Alignment;
import com.vaadin.ui.Composite;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

@SuppressWarnings("serial")
public class FakeGrid extends Composite {

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
        var spinner = new Label();
        spinner.addStyleName(ValoTheme.LABEL_SPINNER);
        spinnerWrapper.addComponent(spinner);
        spinnerWrapper.setComponentAlignment(spinner, Alignment.MIDDLE_CENTER);
        layout.addComponents(fakeHeader, spinnerWrapper);
        layout.setExpandRatio(spinnerWrapper, 1);
    }
}
