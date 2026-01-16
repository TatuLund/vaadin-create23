package org.vaadin.tatu.vaadincreate.about;

import java.io.Serializable;
import java.time.LocalDateTime;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.tatu.vaadincreate.VaadinCreateUI;
import org.vaadin.tatu.vaadincreate.backend.AppDataService;
import org.vaadin.tatu.vaadincreate.backend.data.Message;
import org.vaadin.tatu.vaadincreate.backend.events.AbstractEvent;
import org.vaadin.tatu.vaadincreate.backend.events.MessageEvent;
import org.vaadin.tatu.vaadincreate.backend.events.ShutdownEvent;
import org.vaadin.tatu.vaadincreate.eventbus.EventBus;
import org.vaadin.tatu.vaadincreate.eventbus.EventBus.EventBusListener;

@NullMarked
public class AboutPresenter implements EventBusListener, Serializable {

    private static final Logger logger = LoggerFactory
            .getLogger(AboutPresenter.class);

    private final AboutView view;

    /**
     * Constructor.
     *
     * @param view
     *            the about view
     */
    public AboutPresenter(AboutView view) {
        this.view = view;
        getEventBus().registerEventBusListener(this);
    }

    /**
     * Fetches the admin message.
     *
     * @return the admin message, or null if none is set
     */
    public @Nullable Message fetchMessage() {
        return getService().getMessage();
    }

    /**
     * Updates the admin message.
     *
     * @param text
     *            the new message text
     * @return the updated message
     */
    public Message updateMessage(String text) {
        Message mes = getService().updateMessage(text);
        getEventBus()
                .post(new MessageEvent(mes.getMessage(), mes.getDateStamp()));
        return mes;
    }

    private EventBus getEventBus() {
        return EventBus.get();
    }

    private AppDataService getService() {
        return VaadinCreateUI.get().getAppService();
    }

    /**
     * Unregisters this presenter from the event bus.
     */
    public void unregister() {
        getEventBus().unregisterEventBusListener(this);
    }

    /**
     * Schedules a global shutdown.
     */
    public void scheduleShutdown() {
        logger.info("Global logout scheduled in 60 seconds");
        getEventBus().post(new ShutdownEvent());
    }

    @Override
    public void eventFired(AbstractEvent event) {
        switch (event) {
        case MessageEvent(String message, LocalDateTime timeStamp) -> view
                .updateAsync(message, timeStamp);
        case ShutdownEvent shutdownEvent -> view.enableShutdownAsync();
        default -> {
            // No action
        }
        }
    }

}
