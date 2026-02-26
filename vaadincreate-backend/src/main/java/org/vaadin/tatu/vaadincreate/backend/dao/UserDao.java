package org.vaadin.tatu.vaadincreate.backend.dao;

import java.util.List;
import java.util.Objects;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.tatu.vaadincreate.backend.data.PurchaseStatus;
import org.vaadin.tatu.vaadincreate.backend.data.User;

/**
 * Data access object class for managing User entities.
 */
@NullMarked
@SuppressWarnings("java:S1602")
public class UserDao {

    /**
     * Finds a user by their name.
     *
     * @param name
     *            the name of the user to find
     * @return the user with the specified name, or null if no such user exists
     */
    @Nullable
    public User findByName(String name) {
        logger.debug("Finding user {}", name);
        return HibernateUtil.inSession(session -> {
            @Nullable
            User user = session
                    .createQuery("from User where name = :name", User.class)
                    .setParameter("name", name).uniqueResult();
            return user;
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
        logger.debug("Persisting User: ({}) '{}'", user.getId(),
                user.getName());
        return HibernateUtil.saveOrUpdate(user);
    }

    /**
     * Retrieves a User entity from the database based on the provided user ID.
     *
     * @param userId
     *            the ID of the user to be fetched
     * @return the User entity corresponding to the given user ID, or null if no
     *         such user exists
     */
    @Nullable
    public User getUserById(Integer userId) {
        logger.debug("Fetching User: ({})", userId);
        return HibernateUtil.inSession(session -> {
            @Nullable
            User user = session.get(User.class, userId);
            return user;
        });
    }

    /**
     * Removes a user from the database based on the provided user ID.
     *
     * @param userId
     *            the ID of the user to be removed
     */
    @SuppressWarnings("unused")
    public void removeUser(Integer userId) {
        Objects.requireNonNull(userId, "User ID must not be null");
        logger.debug("Deleting User: ({})", userId);
        HibernateUtil.inTransaction(session -> {
            @Nullable
            User user = session.get(User.class, userId);
            if (user == null) {
                throw new IllegalArgumentException(
                        "User to be deleted not found: " + userId);
            }
            session.delete(user);
        });
    }

    /**
     * Retrieves a list of all users from the database.
     *
     * @return a List of User objects representing all users in the database.
     */
    public List<@NonNull User> getAllUsers() {
        logger.debug("Fetching all Users");
        List<@NonNull User> users = HibernateUtil.inSession(session -> {
            return session.createQuery("from User", User.class).list();
        });
        if (users == null) {
            throw new IllegalStateException(
                    "Users list is null, this should not happen");
        }
        return users;
    }

    /**
     * Retrieves a list of users with the specified role from the database.
     *
     * @param role
     *            the role to filter users by
     * @return a List of User objects with the specified role
     */
    public List<@NonNull User> getUsersByRole(User.Role role) {
        Objects.requireNonNull(role, "Role must not be null");
        logger.debug("Fetching Users with role {}", role);
        List<@NonNull User> users = HibernateUtil.inSession(session -> {
            return session
                    .createQuery("from User where role = :role", User.class)
                    .setParameter("role", role).list();
        });
        if (users == null) {
            throw new IllegalStateException(
                    "Users list is null, this should not happen");
        }
        return users;
    }

    /**
     * Returns all active users with role {@code USER} or {@code ADMIN},
     * excluding the given user. Used to populate the deputy-approver selection.
     *
     * @param excludeUser
     *            the user to exclude from the result (the one being edited)
     * @return list of eligible deputy approvers
     */
    public List<@NonNull User> getActiveApprovers(User excludeUser) {
        Objects.requireNonNull(excludeUser, "ExcludeUser must not be null");
        logger.debug("Fetching active approvers excluding user ({})",
                excludeUser.getId());
        List<@NonNull User> users = HibernateUtil.inSession(session -> {
            return session.createQuery(
                    "from User where active = true and role in (:user, :admin) and id != :id",
                    User.class)
                    .setParameter("user", User.Role.USER)
                    .setParameter("admin", User.Role.ADMIN)
                    .setParameter("id", excludeUser.getId()).list();
        });
        if (users == null) {
            throw new IllegalStateException(
                    "Users list is null, this should not happen");
        }
        return users;
    }

    /**
     * Counts the number of currently active users with role {@code ADMIN}.
     *
     * @return count of active admins
     */
    public long countActiveAdmins() {
        logger.debug("Counting active admins");
        var result = HibernateUtil.inSession(session -> {
            @Nullable
            Long count = session.createQuery(
                    "select count(u) from User u where u.role = :role and u.active = true",
                    Long.class).setParameter("role", User.Role.ADMIN)
                    .uniqueResult();
            return count;
        });
        return result != null ? result : 0L;
    }

    /**
     * Atomically deactivates a user and reassigns all their PENDING purchase
     * approvals to a deputy approver in a single transaction.
     *
     * @param editedUser
     *            the user to deactivate (must have {@code active == false} set)
     * @param deputy
     *            the active approver to reassign pending purchases to
     * @return the persisted, deactivated {@link User}
     */
    public User deactivateWithReassignment(User editedUser, User deputy) {
        Objects.requireNonNull(editedUser, "Edited user must not be null");
        Objects.requireNonNull(deputy, "Deputy must not be null");
        logger.info(
                "Deactivating user ({}) with reassignment to deputy ({})",
                editedUser.getId(), deputy.getId());
        var result = HibernateUtil.inTransaction(session -> {
            // Reassign pending purchases from the deactivated user to deputy.
            session.createQuery(
                    "update Purchase set approver = :deputy where approver.id = :userId and status = :status")
                    .setParameter("deputy", deputy)
                    .setParameter("userId", editedUser.getId())
                    .setParameter("status", PurchaseStatus.PENDING)
                    .executeUpdate();
            // Merge the updated (deactivated) user in the same transaction.
            return (User) session.merge(editedUser);
        });
        if (result == null) {
            throw new IllegalStateException(
                    "Result of deactivateWithReassignment is null");
        }
        return result;
    }

    @SuppressWarnings("null")
    private Logger logger = LoggerFactory.getLogger(this.getClass());
}
