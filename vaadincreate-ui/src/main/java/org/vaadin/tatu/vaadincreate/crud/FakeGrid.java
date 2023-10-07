package org.vaadin.tatu.vaadincreate.crud;

import com.vaadin.ui.Alignment;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

@SuppressWarnings("serial")
public class FakeGrid extends VerticalLayout {

    private static final String FAKEGRID_HEADER = "fakegrid-header";
    private static final String FAKEGRID_SPINNERWRAPPER = "fakegrid-spinnerwrapper";
    private static final String FAKEGRID = "fakegrid";

    public FakeGrid() {
        setId("fake-grid");
        addStyleName(FAKEGRID);
        setHeightFull();
        setMargin(false);
        setSpacing(false);
        var spinnerWrapper = new VerticalLayout();
        spinnerWrapper.addStyleName(FAKEGRID_SPINNERWRAPPER);
        spinnerWrapper = new VerticalLayout();
        spinnerWrapper.setHeightFull();
        spinnerWrapper.setMargin(false);
        spinnerWrapper.setSpacing(false);
        var fakeHeader = new CssLayout();
        fakeHeader.addStyleName(FAKEGRID_HEADER);
        var spinner = new Label();
        spinner.addStyleName(ValoTheme.LABEL_SPINNER);
        spinnerWrapper.addComponent(spinner);
        spinnerWrapper.setComponentAlignment(spinner, Alignment.MIDDLE_CENTER);
        addComponents(fakeHeader, spinnerWrapper);
        setExpandRatio(spinnerWrapper, 1);
    }
}
