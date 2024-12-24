package org.vaadin.tatu.vaadincreate;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.annotation.WebServlet;

import com.vaadin.annotations.Theme;
import com.vaadin.annotations.VaadinServletConfiguration;
import com.vaadin.annotations.Widgetset;
import com.vaadin.navigator.Navigator;
import com.vaadin.navigator.View;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinServlet;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

@SuppressWarnings("serial")
@Theme(ValoTheme.THEME_NAME)
@Widgetset(value = "org.vaadin.tatu.vaadincreate.WidgetSet")
public class ComponentsUI extends UI {

    Navigator nav;
    Map<String, Class<? extends View>> views = new HashMap<>();

    @Override
    protected void init(VaadinRequest request) {
        var content = new VerticalLayout();
        nav = new Navigator(this, content);
        addView("", DefaultView.class);
        addView(CharacterCountExtensionView.NAME,
                CharacterCountExtensionView.class);
        addView(ResetButtonForTextFieldView.NAME,
                ResetButtonForTextFieldView.class);
        addView(AttributeExtensionView.NAME, AttributeExtensionView.class);
        addView(CapsLockWarningView.NAME, CapsLockWarningView.class);
        addView(ConfirmDialogView.NAME, ConfirmDialogView.class);
        setContent(content);
    }

    private void addView(String name, Class<? extends View> view) {
        nav.addView(name, view);
        views.put(name, view);
    }

    public Map<String, Class<? extends View>> getViews() {
        return views;
    }

    public void navigate(String name) {
        nav.navigateTo(name);
    }

    @WebServlet(value = "/*", asyncSupported = true)
    @VaadinServletConfiguration(productionMode = false, ui = ComponentsUI.class)
    public static class ComponentsServlet extends VaadinServlet {
    }

}
