package org.vaadin.tatu.vaadincreate.purchases;

import java.lang.reflect.Method;
import java.util.Objects;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.vaadin.tatu.vaadincreate.components.AbstractDialog;
import org.vaadin.tatu.vaadincreate.components.AttributeExtension;
import org.vaadin.tatu.vaadincreate.components.AttributeExtension.AriaAttributes;
import org.vaadin.tatu.vaadincreate.i18n.HasI18N;
import org.vaadin.tatu.vaadincreate.i18n.I18n;

import com.vaadin.event.ConnectorEventListener;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.server.FileDownloader;
import com.vaadin.server.StreamResource;
import com.vaadin.shared.Registration;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;
import com.vaadin.util.ReflectTools;

@NullMarked
@SuppressWarnings({ "serial", "java:S2160" })
public class PurchaseExportDownloadDialog extends AbstractDialog
        implements HasI18N {

    public static final String DOWNLOAD_BUTTON_ID = "export-download-button";

    public PurchaseExportDownloadDialog(StreamResource resource) {
        super();
        window.setId("purchase-export-dialog");
        window.setCaption(getTranslation(I18n.Storefront.EXPORT));
        window.setClosable(true);
        window.setWidth("420px");
        window.setHeightUndefined();
        window.addCloseShortcut(com.vaadin.event.ShortcutAction.KeyCode.ESCAPE);

        var download = new Button(getTranslation(I18n.Storefront.DOWNLOAD));
        download.setId(DOWNLOAD_BUTTON_ID);
        download.setIcon(VaadinIcons.DOWNLOAD);
        download.addStyleNames(ValoTheme.BUTTON_PRIMARY,
                ValoTheme.BUTTON_ICON_ALIGN_TOP);
        new FileDownloader(resource).extend(download);

        var buttons = new HorizontalLayout(download);
        buttons.setSpacing(true);
        var label = new Label(getTranslation(I18n.Storefront.EXPORT_READY));
        label.addStyleName(ValoTheme.LABEL_SUCCESS);
        AttributeExtension.of(label).setAttribute(AriaAttributes.LIVE,
                "polite");
        var content = new VerticalLayout(label, buttons);
        content.setComponentAlignment(label, Alignment.MIDDLE_CENTER);
        content.setMargin(true);
        content.setSpacing(true);
        content.setComponentAlignment(buttons, Alignment.MIDDLE_CENTER);
        window.setContent(content);
        window.addCloseListener(e -> fireEvent(new ClosedEvent(this)));
    }

    public Registration addCloseListener(ClosedListener listener) {
        Objects.requireNonNull(listener, "Listener must not be null");
        assert ClosedListener.CLOSED_METHOD != null : "Closed method is null";
        return addListener(ClosedEvent.class, listener,
                ClosedListener.CLOSED_METHOD);
    }

    public interface ClosedListener extends ConnectorEventListener {
        @Nullable
        Method CLOSED_METHOD = ReflectTools.findMethod(ClosedListener.class,
                "closed", ClosedEvent.class);

        void closed(ClosedEvent event);
    }

    public static class ClosedEvent extends Component.Event {
        public ClosedEvent(Component source) {
            super(source);
        }
    }
}
