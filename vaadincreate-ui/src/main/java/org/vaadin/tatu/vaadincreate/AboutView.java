package org.vaadin.tatu.vaadincreate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.tatu.vaadincreate.auth.AccessControl;
import org.vaadin.tatu.vaadincreate.auth.AllPermitted;
import org.vaadin.tatu.vaadincreate.backend.AppDataService;
import org.vaadin.tatu.vaadincreate.backend.data.Message;
import org.vaadin.tatu.vaadincreate.backend.data.User.Role;
import org.vaadin.tatu.vaadincreate.eventbus.EventBus;
import org.vaadin.tatu.vaadincreate.eventbus.EventBus.EventBusListener;
import org.vaadin.tatu.vaadincreate.i18n.HasI18N;
import org.vaadin.tatu.vaadincreate.util.Utils;

import com.vaadin.data.HasValue.ValueChangeEvent;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.shared.Version;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.shared.ui.ValueChangeMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.CustomLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

@AllPermitted
@SuppressWarnings("serial")
public class AboutView extends VerticalLayout
        implements View, EventBusListener, HasI18N {

    public static final String VIEW_NAME = "about";

    private static final String VAADIN = "vaadin";

    private AppDataService service = VaadinCreateUI.get().getAppService();
    private AccessControl accessControl = VaadinCreateUI.get()
            .getAccessControl();

    private Button editButton;
    private Label adminsNote;
    private TextArea textArea;

    private EventBus eventBus = EventBus.get();

    public AboutView() {
        var aboutContent = createAboutContent();

        textArea = createTextArea();
        textArea.setVisible(false);

        var adminsContent = new HorizontalLayout();
        adminsContent.addStyleName(VaadinCreateTheme.ABOUTVIEW_ADMINSCONTENT);
        adminsContent.setWidth("500px");
        createAdminsNote();

        adminsContent.addComponents(adminsNote, textArea);
        if (accessControl.isUserInRole(Role.ADMIN)) {
            createEditButton();
            editButton.addClickListener(e -> {
                textArea.setVisible(true);
                adminsNote.setVisible(false);
                editButton.setVisible(false);
                textArea.setValue(adminsNote.getValue());
            });
            adminsContent.addComponent(editButton);
            adminsContent.setComponentAlignment(editButton,
                    Alignment.TOP_RIGHT);
        }

        textArea.addValueChangeListener(this::handleValueChange);
        textArea.setValueChangeMode(ValueChangeMode.BLUR);
        textArea.addBlurListener(e -> {
            adminsNote.setVisible(true);
            textArea.setVisible(false);
            editButton.setVisible(true);
        });
        setSizeFull();
        setMargin(false);
        setStyleName(VaadinCreateTheme.ABOUT_VIEW);
        addComponents(aboutContent, adminsContent);
        setComponentAlignment(aboutContent, Alignment.MIDDLE_CENTER);
        setComponentAlignment(adminsContent, Alignment.MIDDLE_CENTER);
        eventBus.registerEventBusListener(this);
    }

    private void handleValueChange(ValueChangeEvent<String> e) {
        if (e.isUserOriginated()) {
            var unsanitized = e.getValue();
            // Sanitize user input with Jsoup to avoid JavaScript injection
            // vulnerabilities
            var text = Utils.sanitize(unsanitized);
            Message mes = service.updateMessage(text);
            adminsNote.setCaption(
                    Utils.formatDate(mes.getDateStamp(), getLocale()));
            adminsNote.setValue(mes.getMessage());
            eventBus.post(mes);
            logger.info("Admin message updated");
        }
    }

    private void createEditButton() {
        editButton = new Button();
        editButton.setId("admin-edit");
        editButton.setIcon(VaadinIcons.EDIT);
        editButton.addStyleNames(ValoTheme.BUTTON_BORDERLESS,
                ValoTheme.BUTTON_SMALL);
    }

    private void createAdminsNote() {
        adminsNote = new Label();
        adminsNote.setContentMode(ContentMode.HTML);
        adminsNote.addStyleName(VaadinCreateTheme.WHITESPACE_PRE);
        adminsNote.setId("admins-note");
    }

    // Create text area for editing admins note
    private TextArea createTextArea() {
        var textArea = new TextArea();
        textArea.setId("admins-text-area");
        textArea.setMaxLength(250);
        textArea.setWidth("450px");
        textArea.setIcon(VaadinIcons.FILE_TEXT_O);
        textArea.setCaption("HTML");
        textArea.setPlaceholder("max 250 chars");
        CharacterCountExtension.extend(textArea);
        return textArea;
    }

    private CustomLayout createAboutContent() {
        var aboutContent = new CustomLayout("aboutview");
        aboutContent.setStyleName(VaadinCreateTheme.ABOUTVIEW_ABOUTCONTENT);

        var aboutLabel = new Label(
                VaadinIcons.INFO_CIRCLE.getHtml()
                        + getTranslation(VAADIN, Version.getFullVersion()),
                ContentMode.HTML);
        aboutLabel.addStyleName(VaadinCreateTheme.ABOUTVIEW_ABOUTLABEL);
        // you can add Vaadin components in predefined slots in the custom
        // layout
        aboutContent.addComponent(aboutLabel, "info");
        return aboutContent;
    }

    @Override
    public void enter(ViewChangeEvent event) {
        Message message = service.getMessage();
        adminsNote.setCaption(
                Utils.formatDate(message.getDateStamp(), getLocale()));
        adminsNote.setValue(message.getMessage());
    }

    @Override
    public void eventFired(Object event) {
        if (event instanceof Message) {
            getUI().access(() -> {
                if (textArea.isVisible()) {
                    textArea.setVisible(false);
                    adminsNote.setVisible(true);
                    editButton.setVisible(true);
                }
                Message mes = (Message) event;
                adminsNote.setCaption(
                        Utils.formatDate(mes.getDateStamp(), getLocale()));
                adminsNote.setValue(mes.getMessage());
            });
        }
    }

    @Override
    public void detach() {
        super.detach();
        eventBus.unregisterEventBusListener(this);
    }

    private Logger logger = LoggerFactory.getLogger(this.getClass());
}
