package org.vaadin.tatu.vaadincreate;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.server.ErrorHandlingRunnable;
import com.vaadin.ui.UIDetachedException;

@NullMarked
public class AccessTask implements ErrorHandlingRunnable {

    @Nullable
    private final transient Runnable command;

    public AccessTask(Runnable command) {
        this.command = command;
    }

    @Override
    public void handleError(Exception exception) {
        if (exception instanceof UIDetachedException) {
            logger.info("Browser window was closed while pushing updates.");
        } else {
            logger.error("Error while pushing updates", exception);
        }
    }

    @Override
    public void run() {
        command.run();
    }

    private static Logger logger = LoggerFactory.getLogger(AccessTask.class);
}
