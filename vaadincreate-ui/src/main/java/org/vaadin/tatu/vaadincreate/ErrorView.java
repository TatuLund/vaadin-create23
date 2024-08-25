package org.vaadin.tatu.vaadincreate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.tatu.vaadincreate.auth.AllPermitted;
import org.vaadin.tatu.vaadincreate.auth.CurrentUser;
import org.vaadin.tatu.vaadincreate.i18n.HasI18N;
import org.vaadin.tatu.vaadincreate.i18n.I18n;

import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

/**
 * View shown when trying to navigate to a view that does not exist using
 * {@link com.vaadin.navigator.Navigator}.
 */
@SuppressWarnings({ "serial", "java:S2160" })
@AllPermitted
public class ErrorView extends VerticalLayout implements View, HasI18N {

    Label explanation = new Label();

    public ErrorView() {
        var header = new Label(getTranslation(I18n.Error.VIEW_NOT_FOUND));
        header.addStyleName(ValoTheme.LABEL_H1);
        addComponent(header);
        addComponent(explanation);
    }

    @Override
    public void enter(ViewChangeListener.ViewChangeEvent event) {
        explanation.setValue(
                getTranslation(I18n.Error.NOT_FOUND_DESC, event.getViewName()));
        logger.warn("User '{}' attempted to navigate non-existent view '{}'",
                CurrentUser.get().get().getName(), event.getViewName());
    }

    private static Logger logger = LoggerFactory.getLogger(ErrorView.class);
}
