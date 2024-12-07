package org.vaadin.tatu.vaadincreate.backend.dao;

import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.tatu.vaadincreate.backend.data.Message;

@SuppressWarnings("java:S1602")
public class MessageDao {

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

    public Message getLastMessage() {
        logger.info("Fetching Message");
        return HibernateUtil.inSession(session -> {
            return session
                    .createQuery("from Message order by id desc", Message.class)
                    .setMaxResults(1).uniqueResult();
        });
    }

    public Collection<Message> getMessages() {
        logger.info("Fetching all Messages");
        return HibernateUtil.inSession(session -> {
            return session.createQuery("from Message", Message.class).list();
        });
    }

    private Logger logger = LoggerFactory.getLogger(this.getClass());
}
