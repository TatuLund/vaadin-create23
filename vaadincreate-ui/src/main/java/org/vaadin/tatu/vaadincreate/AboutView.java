package org.vaadin.tatu.vaadincreate;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document.OutputSettings;
import org.jsoup.safety.Safelist;
import org.vaadin.tatu.vaadincreate.auth.AccessAllowed;
import org.vaadin.tatu.vaadincreate.backend.AppDataService;
import org.vaadin.tatu.vaadincreate.backend.data.Message;
import org.vaadin.tatu.vaadincreate.backend.data.User.Role;

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

@AccessAllowed({ Role.USER, Role.ADMIN })
@SuppressWarnings("serial")
public class AboutView extends VerticalLayout implements View {

    public static final String VIEW_NAME = "about";
    private Button editButton;
    private Label adminsNote;

    public AboutView() {
        var aboutContent = new CustomLayout("aboutview");
        aboutContent.setStyleName(VaadinCreateTheme.ABOUTVIEW_ABOUTCONTENT);

        // you can add Vaadin components in predefined slots in the custom
        // layout
        aboutContent.addComponent(new Label(VaadinIcons.INFO_CIRCLE.getHtml()
                + " This application is using Vaadin "
                + Version.getFullVersion(), ContentMode.HTML), "info");

        var textField = new TextArea();
        textField.setMaxLength(250);
        textField.setWidth("450px");
        textField.setIcon(VaadinIcons.FILE_TEXT_O);
        textField.setCaption("HTML");
        textField.setPlaceholder("max 250 chars");
        CharacterCountExtension.extend(textField);
        textField.setVisible(false);

        var adminsContent = new HorizontalLayout();
        adminsContent.addStyleName(VaadinCreateTheme.ABOUTVIEW_ADMINSCONTENT);
        adminsContent.setWidth("500px");
        adminsNote = new Label();

        adminsNote.setContentMode(ContentMode.HTML);
        adminsNote.addStyleName(VaadinCreateTheme.WHITESPACE_PRE);
        adminsContent.addComponents(adminsNote, textField);
        if (VaadinCreateUI.get().getAccessControl().isUserInRole(Role.ADMIN)) {
            editButton = new Button();
            editButton.setIcon(VaadinIcons.EDIT);
            editButton.addStyleNames(ValoTheme.BUTTON_BORDERLESS,
                    ValoTheme.BUTTON_SMALL);
            editButton.addClickListener(e -> {
                textField.setVisible(true);
                adminsNote.setVisible(false);
                editButton.setVisible(false);
                textField.setValue(adminsNote.getValue());
            });
            adminsContent.addComponent(editButton);
            adminsContent.setComponentAlignment(editButton,
                    Alignment.TOP_RIGHT);
        }

        textField.addValueChangeListener(e -> {
            if (e.isUserOriginated()) {
                var settings = new OutputSettings();
                settings.prettyPrint(false);
                var text = Jsoup.clean(e.getValue(), "", Safelist.relaxed(),
                        settings);
                Message mes = AppDataService.get().updateMessage(text);
                adminsNote.setCaption(mes.getDateStamp().toString());
                adminsNote.setValue(mes.getMessage());
            }
        });
        textField.setValueChangeMode(ValueChangeMode.BLUR);
        textField.addBlurListener(e -> {
            adminsNote.setVisible(true);
            textField.setVisible(false);
            editButton.setVisible(true);
        });
        setSizeFull();
        setMargin(false);
        setStyleName(VaadinCreateTheme.ABOUT_VIEW);
        addComponents(aboutContent, adminsContent);
        setComponentAlignment(aboutContent, Alignment.MIDDLE_CENTER);
        setComponentAlignment(adminsContent, Alignment.MIDDLE_CENTER);
    }

    @Override
    public void enter(ViewChangeEvent event) {
        Message message = AppDataService.get().getMessage();
        adminsNote.setCaption(message.getDateStamp().toString());
        adminsNote.setValue(message.getMessage());
    }

}
