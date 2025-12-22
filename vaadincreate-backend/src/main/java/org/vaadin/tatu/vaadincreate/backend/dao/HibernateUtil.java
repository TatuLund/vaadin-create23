package org.vaadin.tatu.vaadincreate.backend.dao;

import java.util.function.Consumer;
import java.util.function.Function;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.exception.JDBCConnectionException;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.tatu.vaadincreate.backend.DatabaseConnectionException;

/**
 * Utility class for managing Hibernate sessions and transactions.
 */
@NullMarked
public class HibernateUtil {

    private static final int DATABASE_CALL_WARN_LIMIT = 100;

    // Private constructor to prevent instantiation
    private HibernateUtil() {
        throw new UnsupportedOperationException("Utility class");
    }

    @Nullable
    static SessionFactory sessionFactory;

    static {
        try {
            String hibernateConfig = System.getProperty("hibernate.config",
                    "hibernate.cfg.xml");
            sessionFactory = new Configuration().configure(hibernateConfig)
                    .buildSessionFactory();
        } catch (Exception ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    /**
     * Retrieves the singleton instance of the SessionFactory.
     *
     * @return the SessionFactory instance used for creating Hibernate sessions.
     */
    @SuppressWarnings("null")
    public static synchronized SessionFactory getSessionFactory() {
        if (sessionFactory == null) {
            throw new IllegalStateException("SessionFactory was shut down");
        }
        return sessionFactory;
    }

    /**
     * Executes a function within a database transaction.
     * 
     * @param <T>
     *            The type of the result returned by the transaction function.
     * @param transaction
     *            A function that takes a Hibernate {@link Session} and returns
     *            a result.
     * @return The result of the transaction function.
     * @throws Exception
     *             if the transaction fails and is rolled back.
     */
    @Nullable
    public static <T> T inTransaction(
            Function<@NonNull Session, T> transaction) {
        var start = System.currentTimeMillis();
        T result;
        Session session = null;
        Transaction tx = null;
        try {
            session = getSessionFactory().openSession();
            tx = session.beginTransaction();
            result = transaction.apply(session);
            tx.commit();
        } catch (Exception e) {
            if (tx != null) {
                tx.rollback();
            }
            handleDatabaseException(e);
            throw e;
        } finally {
            if (session != null) {
                session.close();
            }
        }
        logWarning(start);
        return result;
    }

    /**
     * Executes a given transaction within a Hibernate session.
     * <p>
     * This method opens a new session, begins a transaction, and executes the
     * provided {@link Consumer} with the session. If the transaction is
     * successful, it commits the transaction. If an exception occurs, it rolls
     * back the transaction and rethrows the exception. The session is closed in
     * the finally block to ensure it is always closed.
     *
     * @param transaction
     *            the {@link Consumer} that contains the operations to be
     *            performed within the transaction
     * @throws Exception
     *             if an error occurs during the transaction, it is propagated
     *             after rolling back the transaction
     */
    public static void inTransaction(Consumer<@NonNull Session> transaction) {
        var start = System.currentTimeMillis();
        Session session = null;
        Transaction tx = null;
        try {
            session = getSessionFactory().openSession();
            tx = session.beginTransaction();
            transaction.accept(session);
            tx.commit();
        } catch (Exception e) {
            if (tx != null) {
                tx.rollback();
            }
            handleDatabaseException(e);
            throw e;
        } finally {
            if (session != null) {
                session.close();
            }
        }
        logWarning(start);
    }

    /**
     * Executes a task within a Hibernate session and ensures the session is
     * closed after the task is completed.
     *
     * @param <T>
     *            The type of the result returned by the task.
     * @param task
     *            A function that takes a Hibernate {@link Session} and returns
     *            a result of type T.
     * @return The result of the task.
     */
    @Nullable
    public static <T> T inSession(Function<Session, T> task) {
        var start = System.currentTimeMillis();
        T result;
        Session session = null;
        try {
            session = getSessionFactory().openSession();
            result = task.apply(session);
        } catch (Exception e) {
            handleDatabaseException(e);
            throw e;
        } finally {
            if (session != null) {
                session.close();
            }
        }
        logWarning(start);
        return result;
    }

    /**
     * Executes a task within a Hibernate session. The session is opened before
     * the task is executed and closed after the task completes, ensuring proper
     * resource management.
     *
     * @param task
     *            a {@link Consumer} that accepts a {@link Session} and performs
     *            operations within that session.
     */
    public static void inSession(Consumer<Session> task) {
        var start = System.currentTimeMillis();
        Session session = null;
        try {
            session = getSessionFactory().openSession();
            task.accept(session);
        } catch (Exception e) {
            handleDatabaseException(e);
            throw e;
        } finally {
            if (session != null) {
                session.close();
            }
        }
        logWarning(start);
    }

    private static void handleDatabaseException(Exception e)
            throws DatabaseConnectionException {
        // handle JDBC connection issues
        if (e instanceof JDBCConnectionException) {
            throw new DatabaseConnectionException("Database connection error",
                    e);
        }
    }

    private static void logWarning(long start) {
        var time = System.currentTimeMillis() - start;
        if (time > DATABASE_CALL_WARN_LIMIT) {
            logger.warn("Database call duration exceeded {}ms, {}ms.",
                    DATABASE_CALL_WARN_LIMIT, time);
        }
    }

    /**
     * Closes the SessionFactory and releases all resources.
     */
    @SuppressWarnings("null")
    public static synchronized void shutdown() {
        if (sessionFactory == null) {
            return;
        }
        sessionFactory.close();
        sessionFactory = null;
    }

    @SuppressWarnings("null")
    private static Logger logger = LoggerFactory.getLogger(HibernateUtil.class);

}
