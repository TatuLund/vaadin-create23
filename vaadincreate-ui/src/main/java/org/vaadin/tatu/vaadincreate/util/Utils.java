package org.vaadin.tatu.vaadincreate.util;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document.OutputSettings;
import org.jsoup.safety.Safelist;

import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinServletRequest;
import com.vaadin.shared.communication.PushMode;
import com.vaadin.ui.UI;

public class Utils {

    public static String sanitize(String unsanitized) {
        var settings = new OutputSettings();
        settings.prettyPrint(false);
        var text = Jsoup.clean(unsanitized, "", Safelist.relaxed()
                .addAttributes("span", "style").addAttributes("span", "class"),
                settings);
        return text;
    }

    public static void sessionFixation() {
        UI.getCurrent().getPushConfiguration().setPushMode(PushMode.DISABLED);
        VaadinServletRequest request = (VaadinServletRequest) VaadinRequest
                .getCurrent();
        if (request != null) {
            request.getHttpServletRequest().changeSessionId();
        }
        UI.getCurrent().getPushConfiguration().setPushMode(PushMode.AUTOMATIC);
    }
}
