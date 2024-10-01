package org.vaadin.tatu.vaadincreate.backend.dao;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.tatu.vaadincreate.backend.data.User;

/**
 * Data access object class for managing User entities.
 */
@SuppressWarnings("java:S1602")
public class UserDao {

    /**
     * Finds a user by their name.
     *
     * @param name
     *            the name of the user to find
     * @return the user with the specified name, or null if no such user exists
     */
    public User findByName(String name) {
        logger.info("Finding user {}", name);
        return HibernateUtil.inSession(session -> {
            return session
                    .createQuery("from User where name = :name", User.class)
                    .setParameter("name", name).uniqueResult();
        });
    }

    /**
     * Updates the given user in the database. If the user already has an ID, it
     * updates the existing record. Otherwise, it saves the new user and assigns
     * an ID to it.
     *
     * @param user
     *            the User object to be updated or saved
     * @return the updated User object retrieved from the database
     */
    public User updateUser(User user) {
        logger.info("Persisting User: ({}) '{}'", user.getId(), user.getName());
        var identifier = HibernateUtil.inTransaction(session -> {
            Integer id;
            if (user.getId() != null) {
                session.update(user);
                id = user.getId();
            } else {
                id = (Integer) session.save(user);
            }
            return id;
        });
        return HibernateUtil.inSession(session -> {
            return session.get(User.class, identifier);
        });
    }

    /**
     * Retrieves a User entity from the database based on the provided user ID.
     *
     * @param userId
     *            the ID of the user to be fetched
     * @return the User entity corresponding to the given user ID, or null if no
     *         such user exists
     */
    public User getUserById(Integer userId) {
        logger.info("Fetching User: ({})", userId);
        return HibernateUtil.inSession(session -> {
            return session.get(User.class, userId);
        });
    }

    /**
     * Removes a user from the database based on the provided user ID.
     *
     * @param userId
     *            the ID of the user to be removed
     */
    public void removeUser(Integer userId) {
        logger.info("Deleting User: ({})", userId);
        HibernateUtil.inTransaction(session -> {
            User user = session.get(User.class, userId);
            session.delete(user);
        });
    }

    /**
     * Retrieves a list of all users from the database.
     *
     * @return a List of User objects representing all users in the database.
     */
    public List<User> getAllUsers() {
        logger.info("Fetching all Users");
        return HibernateUtil.inSession(session -> {
            return session.createQuery("from User", User.class).list();
        });
    }

    private Logger logger = LoggerFactory.getLogger(this.getClass());
}
