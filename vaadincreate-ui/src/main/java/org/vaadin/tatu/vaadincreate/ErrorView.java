package org.vaadin.tatu.vaadincreate;

import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.tatu.vaadincreate.auth.AllPermitted;
import org.vaadin.tatu.vaadincreate.i18n.HasI18N;
import org.vaadin.tatu.vaadincreate.i18n.I18n;
import org.vaadin.tatu.vaadincreate.util.Utils;

import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

/**
 * View shown when trying to navigate to a view that does not exist using
 * {@link com.vaadin.navigator.Navigator}.
 */
@NullMarked
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
    public void enter(ViewChangeListener.ViewChangeEvent viewChange) {
        if (viewChange.getViewName().isEmpty()) {
            getUI().getNavigator().navigateTo(AboutView.VIEW_NAME);
            return;
        }
        explanation.setValue(getTranslation(I18n.Error.NOT_FOUND_DESC,
                viewChange.getViewName()));
        var user = Utils.getCurrentUserOrThrow();
        logger.warn("User '{}' attempted to navigate non-existent view '{}'",
                user.getName(), viewChange.getViewName());
    }

    private static Logger logger = LoggerFactory.getLogger(ErrorView.class);
}
