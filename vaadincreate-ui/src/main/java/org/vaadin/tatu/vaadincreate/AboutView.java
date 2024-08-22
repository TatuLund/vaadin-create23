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
@SuppressWarnings({ "serial", "java:S2160" })
public class AboutView extends VerticalLayout
        implements View, EventBusListener, HasI18N {

    public static final String VIEW_NAME = "about";

    // Localization constants
    private static final String EDIT_NOTE = "edit-note";
    private static final String VAADIN = "vaadin";

    private AccessControl accessControl = VaadinCreateUI.get()
            .getAccessControl();

    private Button editButton;
    private Label adminsNote;
    private TextArea admionsNoteField;

    public AboutView() {
        var aboutContent = createAboutContent();

        admionsNoteField = createTextArea();
        admionsNoteField.setVisible(false);

        var adminsContent = new HorizontalLayout();
        adminsContent.addStyleName(VaadinCreateTheme.ABOUTVIEW_ADMINSCONTENT);
        adminsContent.setWidth("500px");
        createAdminsNote();

        adminsContent.addComponents(adminsNote, admionsNoteField);
        if (accessControl.isUserInRole(Role.ADMIN)) {
            createEditButton();
            editButton.addClickListener(e -> {
                admionsNoteField.setVisible(true);
                adminsNote.setVisible(false);
                editButton.setVisible(false);
                admionsNoteField.setValue(adminsNote.getValue());
            });
            adminsContent.addComponent(editButton);
            adminsContent.setComponentAlignment(editButton,
                    Alignment.TOP_RIGHT);
        }

        admionsNoteField.addValueChangeListener(this::handleValueChange);
        admionsNoteField.setValueChangeMode(ValueChangeMode.BLUR);
        admionsNoteField.addBlurListener(e -> {
            adminsNote.setVisible(true);
            admionsNoteField.setVisible(false);
            editButton.setVisible(true);
        });
        setSizeFull();
        setMargin(false);
        setStyleName(VaadinCreateTheme.ABOUT_VIEW);
        addComponents(aboutContent, adminsContent);
        setComponentAlignment(aboutContent, Alignment.MIDDLE_CENTER);
        setComponentAlignment(adminsContent, Alignment.MIDDLE_CENTER);
        getEventBus().registerEventBusListener(this);
    }

    private void handleValueChange(ValueChangeEvent<String> e) {
        if (e.isUserOriginated()) {
            var unsanitized = e.getValue();
            // Sanitize user input with Jsoup to avoid JavaScript injection
            // vulnerabilities
            var text = Utils.sanitize(unsanitized);
            Message mes = getService().updateMessage(text);
            adminsNote.setCaption(
                    Utils.formatDate(mes.getDateStamp(), getLocale()));
            adminsNote.setValue(mes.getMessage());
            getEventBus().post(mes);
            logger.info("Admin message updated");
        }
    }

    private void createEditButton() {
        editButton = new Button();
        editButton.setId("admin-edit");
        editButton.setIcon(VaadinIcons.EDIT);
        editButton.addStyleNames(ValoTheme.BUTTON_BORDERLESS,
                ValoTheme.BUTTON_SMALL);
        editButton.setDescription(getTranslation(EDIT_NOTE));
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
        Message message = getService().getMessage();
        adminsNote.setCaption(
                Utils.formatDate(message.getDateStamp(), getLocale()));
        adminsNote.setValue(message.getMessage());
    }

    @Override
    public void eventFired(Object event) {
        if (event instanceof Message) {
            getUI().access(() -> {
                if (admionsNoteField.isVisible()) {
                    admionsNoteField.setVisible(false);
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
        getEventBus().unregisterEventBusListener(this);
    }

    private EventBus getEventBus() {
        return EventBus.get();
    }

    private AppDataService getService() {
        return VaadinCreateUI.get().getAppService();
    }

    private static Logger logger = LoggerFactory.getLogger(AboutView.class);
}
