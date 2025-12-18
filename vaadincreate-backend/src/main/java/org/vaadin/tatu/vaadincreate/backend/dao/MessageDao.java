package org.vaadin.tatu.vaadincreate.backend.dao;

import java.util.Collection;
import java.util.Objects;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.tatu.vaadincreate.backend.data.Message;

/**
 * Data access object for managing messages.
 */
@NullMarked
@SuppressWarnings("java:S1602")
public class MessageDao {

    /**
     * Updates the given message in the database. If the message has an ID, it
     * will be updated. Otherwise, a new message will be saved.
     *
     * @param message
     *            the message to be updated or saved
     * @return the updated or newly saved message
     */

    public Message updateMessage(Message message) {
        Objects.requireNonNull(message,
                "Message to be updated must not be null");
        logger.info("Persisting Message: ({}) '{}'", message.getId(),
                message.getMessage());
        var identifier = HibernateUtil.inTransaction(session -> {
            Integer id;
            if (message.getId() != null) {
                session.update(message);
                id = message.getId();
            } else {
                id = (Integer) session.save(message);
            }
            return id;
        });
        var result = HibernateUtil.inSession(session -> {
            @Nullable
            Message msg = session.get(Message.class, identifier);
            return msg;
        });
        if (result == null) {
            throw new IllegalStateException(
                    "Just saved Message is null, this should not happen");
        }
        return result;
    }

    /**
     * Retrieves the last message from the database.
     * 
     * This method fetches the most recent message by ordering the messages by
     * their ID in descending order and returning the first result.
     * 
     * @return the last {@link Message} from the database, or {@code null} if no
     *         messages are found.
     */
    @Nullable
    public Message getLastMessage() {
        logger.info("Fetching Message");
        return HibernateUtil.inSession(session -> {
            @Nullable
            Message msg = session
                    .createQuery("from Message order by id desc", Message.class)
                    .setMaxResults(1).uniqueResult();
            return msg;
        });
    }

    /**
     * Retrieves all messages from the database.
     *
     * @return a collection of all messages.
     */
    public Collection<@NonNull Message> getMessages() {
        logger.info("Fetching all Messages");
        var result = HibernateUtil.inSession(session -> {
            return session.createQuery("from Message", Message.class).list();
        });
        return Objects.requireNonNull(result, "Result of getMessages is null");
    }

    private Logger logger = Objects
            .requireNonNull(LoggerFactory.getLogger(this.getClass()));
}
