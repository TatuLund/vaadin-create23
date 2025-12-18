package org.vaadin.tatu.vaadincreate.backend.dao;

import java.util.Objects;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.vaadin.tatu.vaadincreate.backend.data.Draft;
import org.vaadin.tatu.vaadincreate.backend.data.User;

/**
 * Data access object for managing drafts.
 */
@SuppressWarnings("java:S1602")
@NullMarked
public class DraftDao {

    /**
     * Updates the given Draft object in the database. If the Draft object has
     * an ID, it will be updated; otherwise, it will be saved as a new entry.
     *
     * @param draft
     *            the Draft object to be updated or saved
     * @return the updated or newly saved Draft object retrieved from the
     *         database
     */
    public Draft updateDraft(Draft draft) {
        Objects.requireNonNull(draft, "Draft to be updated must not be null");
        logger.info("Persisting Draft: ({}) '{}'", draft.getId(),
                draft.getProductName());
        var identifier = HibernateUtil.inTransaction(session -> {
            Integer id;
            if (draft.getId() != null) {
                session.update(draft);
                id = draft.getId();
            } else {
                id = (Integer) session.save(draft);
            }
            return id;
        });
        var result = HibernateUtil.inSession(session -> {
            @Nullable
            Draft d = session.get(Draft.class, identifier);
            return d;
        });
        if (result == null) {
            throw new IllegalStateException(
                    "Just saved Draft is null, this should not happen");
        }
        return result;
    }

    /**
     * Deletes the draft associated with the specified user.
     *
     * @param user
     *            the user whose draft is to be deleted
     */
    public void deleteDraft(User user) {
        logger.info("Deleting Draft for User: ({})", user.getId());
        HibernateUtil.inTransaction(session -> {
            session.createQuery("delete from Draft where user_id = :user_id")
                    .setParameter("user_id", user.getId()).executeUpdate();
        });
    }

    /**
     * Finds and returns the Draft associated with the given User.
     *
     * @param user
     *            the User for whom the Draft is to be fetched
     * @return the Draft associated with the given User, or null if no such
     *         Draft exists
     */
    @Nullable
    public Draft findDraft(User user) {
        Objects.requireNonNull(user, "User must not be null");
        var id = Objects.requireNonNull(user.getId(),
                "User ID must not be null");
        logger.info("Fetching Draft for User: ({})", id);
        return HibernateUtil.inSession(session -> {
            @Nullable
            Draft draft = session
                    .createQuery("from Draft where user_id = :user_id",
                            Draft.class)
                    .setParameter("user_id", id).uniqueResult();
            return draft;
        });
    }

    private Logger logger = Objects
            .requireNonNull(LoggerFactory.getLogger(this.getClass()));
}
