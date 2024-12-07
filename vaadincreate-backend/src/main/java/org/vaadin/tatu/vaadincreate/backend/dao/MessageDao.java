package org.vaadin.tatu.vaadincreate.backend.dao;

import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.tatu.vaadincreate.backend.data.Message;

/**
 * Data access object for managing messages.
 */
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
        return HibernateUtil.inSession(session -> {
            return session.get(Message.class, identifier);
        });
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
    public Message getLastMessage() {
        logger.info("Fetching Message");
        return HibernateUtil.inSession(session -> {
            return session
                    .createQuery("from Message order by id desc", Message.class)
                    .setMaxResults(1).uniqueResult();
        });
    }

    /**
     * Retrieves all messages from the database.
     *
     * @return a collection of all messages.
     */
    public Collection<Message> getMessages() {
        logger.info("Fetching all Messages");
        return HibernateUtil.inSession(session -> {
            return session.createQuery("from Message", Message.class).list();
        });
    }

    private Logger logger = LoggerFactory.getLogger(this.getClass());
}
