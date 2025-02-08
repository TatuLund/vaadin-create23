package org.vaadin.tatu.vaadincreate.backend;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.vaadin.tatu.vaadincreate.backend.data.Message;
import org.vaadin.tatu.vaadincreate.backend.service.AppDataServiceImpl;

/**
 * Back-end service interface for retrieving and updating app data.
 */
@NullMarked
public interface AppDataService {

    /**
     * Saves a new message to the database.
     *
     * @param message
     *            text
     * @return the updated or newly saved message
     */
    public abstract Message updateMessage(String message);

    /**
     * Retrieves the last message from the database.
     * 
     * @return the last {@link Message} from the database, or {@code null} if no
     *         messages are found.
     */
    @Nullable
    public abstract Message getMessage();

    public static AppDataService get() {
        return AppDataServiceImpl.getInstance();
    }

}
