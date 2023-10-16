package org.vaadin.tatu.vaadincreate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.tatu.vaadincreate.auth.AllPermitted;
import org.vaadin.tatu.vaadincreate.auth.CurrentUser;

import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

/**
 * View shown when trying to navigate to a view that does not exist using
 * {@link com.vaadin.navigator.Navigator}.
 */
@SuppressWarnings("serial")
@AllPermitted
public class ErrorView extends VerticalLayout implements View {

    private Label explanation;

    public ErrorView() {
        var header = new Label("The view could not be found");
        header.addStyleName(ValoTheme.LABEL_H1);
        addComponent(header);
        addComponent(explanation = new Label());
    }

    @Override
    public void enter(ViewChangeListener.ViewChangeEvent event) {
        explanation.setValue(String.format(
                "You tried to navigate to a view ('%s') that does not exist.",
                event.getViewName()));
        logger.warn("User '{}' attempted to navigate non-existent view '{}'",
                CurrentUser.get().get().getName(), event.getViewName());
    }

    private Logger logger = LoggerFactory.getLogger(this.getClass());
}
