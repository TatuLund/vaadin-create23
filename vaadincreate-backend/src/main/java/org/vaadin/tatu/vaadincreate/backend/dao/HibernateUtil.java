package org.vaadin.tatu.vaadincreate.backend.dao;

import java.util.function.Consumer;
import java.util.function.Function;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

/**
 * Utility class for managing Hibernate sessions and transactions.
 */
public class HibernateUtil {

    // Private constructor to prevent instantiation
    private HibernateUtil() {
        throw new UnsupportedOperationException("Utility class");
    }

    private static SessionFactory sessionFactory;

    static {
        try {
            sessionFactory = new Configuration().configure()
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
    public static SessionFactory getSessionFactory() {
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
    public static <T> T inTransaction(Function<Session, T> transaction) {
        var session = getSessionFactory().openSession();
        var tx = session.beginTransaction();
        T result;
        try {
            result = transaction.apply(session);
            tx.commit();
        } catch (Exception e) {
            tx.rollback();
            throw e;
        } finally {
            session.close();
        }
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
    public static void inTransaction(Consumer<Session> transaction) {
        var session = getSessionFactory().openSession();
        var tx = session.beginTransaction();
        try {
            transaction.accept(session);
            tx.commit();
        } catch (Exception e) {
            tx.rollback();
            throw e;
        } finally {
            session.close();
        }
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
    public static <T> T inSession(Function<Session, T> task) {
        T result;
        var session = getSessionFactory().openSession();
        try {
            result = task.apply(session);
        } finally {
            session.close();
        }
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
        var session = getSessionFactory().openSession();
        try {
            task.accept(session);
        } finally {
            session.close();
        }
    }
}