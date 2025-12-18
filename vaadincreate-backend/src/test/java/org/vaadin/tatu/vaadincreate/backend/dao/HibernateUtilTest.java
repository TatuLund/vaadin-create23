package org.vaadin.tatu.vaadincreate.backend.dao;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.exception.JDBCConnectionException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.vaadin.tatu.vaadincreate.backend.DatabaseConnectionException;

/**
 * Test class for {@link HibernateUtil}.
 * 
 * These unit tests use Mockito to mock the Hibernate Session and Transaction
 * objects.
 */
@SuppressWarnings("null")
public class HibernateUtilTest {

    @Mock
    private SessionFactory mockFactory;

    @Mock
    private Session hibernateSession;

    @Mock
    private Transaction transaction;

    // Backup the original session factory to restore it after tests
    // to avoid side effects on other tests
    private SessionFactory backupFactory;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        backupFactory = HibernateUtil.sessionFactory;
        HibernateUtil.sessionFactory = mockFactory;
        when(mockFactory.openSession()).thenReturn(hibernateSession);
        when(hibernateSession.beginTransaction()).thenReturn(transaction);
    }

    @After
    public void tearDown() {
        HibernateUtil.sessionFactory = backupFactory;
    }

    @Test
    public void testInTransactionFunctionSuccess() {
        Function<Session, String> function = session -> "result";
        String result = HibernateUtil.inTransaction(function);
        verify(hibernateSession).beginTransaction();
        verify(transaction).commit();
        verify(hibernateSession).close();
        assertEquals("result", result);
    }

    @Test
    public void testInTransactionConsumerSuccess() {
        var count = new AtomicInteger(0);
        Consumer<Session> consumer = session -> {
            count.incrementAndGet();
        };
        HibernateUtil.inTransaction(consumer);
        verify(hibernateSession).beginTransaction();
        verify(transaction).commit();
        verify(hibernateSession).close();
        assertEquals(1, count.get());
    }

    @Test
    public void testInTransactionFunctionFailure() {
        Function<Session, String> function = session -> {
            throw new RuntimeException("Test exception");
        };

        try {
            HibernateUtil.inTransaction(function);
            fail("Expected RuntimeException was not thrown");
        } catch (RuntimeException e) {
            assertEquals("Test exception", e.getMessage());
        }

        verify(hibernateSession).beginTransaction();
        verify(transaction).rollback();
        verify(hibernateSession).close();
    }

    @Test
    public void testInTransactionConsumerFailure() {
        Consumer<Session> consumer = session -> {
            throw new RuntimeException("Test exception");
        };
        try {
            HibernateUtil.inTransaction(consumer);
            fail("Expected RuntimeException was not thrown");
        } catch (RuntimeException e) {
            assertEquals("Test exception", e.getMessage());
        }

        verify(hibernateSession).beginTransaction();
        verify(transaction).rollback();
        verify(hibernateSession).close();
    }

    @Test
    public void testInSessionFunctionSuccess() {
        Function<Session, String> function = session -> "result";
        String result = HibernateUtil.inSession(function);
        verify(hibernateSession, never()).beginTransaction();
        verify(transaction, never()).commit();
        verify(hibernateSession).close();
        assertEquals("result", result);
    }

    @Test
    public void testInSessionFunctionFailure() {
        Function<Session, String> function = session -> {
            throw new RuntimeException("Test exception");
        };

        try {
            HibernateUtil.inSession(function);
            fail("Expected RuntimeException was not thrown");
        } catch (RuntimeException e) {
            assertEquals("Test exception", e.getMessage());
        }

        verify(hibernateSession).close();
    }

    @Test
    public void testInSessionConsumerSuccess() {
        var count = new AtomicInteger(0);
        Consumer<Session> consumer = session -> {
            count.incrementAndGet();
        };
        HibernateUtil.inSession(consumer);
        verify(hibernateSession, never()).beginTransaction();
        verify(transaction, never()).commit();
        verify(hibernateSession).close();
        assertEquals(1, count.get());
    }

    @Test
    public void testInSessionConsumerFailure() {
        Consumer<Session> consumer = session -> {
            throw new RuntimeException("Test exception");
        };
        try {
            HibernateUtil.inSession(consumer);
            fail("Expected RuntimeException was not thrown");
        } catch (RuntimeException e) {
            assertEquals("Test exception", e.getMessage());
        }

        verify(hibernateSession).close();
    }

    @Test
    public void shutdown() {
        HibernateUtil.shutdown();
        verify(mockFactory).close();
        assertNull(HibernateUtil.sessionFactory);
        // Verify that getSessionFactory() throws an exception
        try {
            HibernateUtil.getSessionFactory();
            fail("Expected IllegalStateException was not thrown");
        } catch (IllegalStateException e) {
            assertEquals("SessionFactory was shut down", e.getMessage());
        }
    }

    @Test
    public void databaseConnectionException() {
        var exception = new JDBCConnectionException("DB error", null);
        when(mockFactory.openSession())
                .thenThrow(exception);
        try {
            HibernateUtil.inSession(session -> "result");
            fail("Expected DatabaseConnectionException was not thrown");
        } catch (DatabaseConnectionException e) {
            assertEquals(exception, e.getCause());
        }
        try {
            HibernateUtil.inTransaction(session -> "result");
            fail("Expected DatabaseConnectionException was not thrown");
        } catch (DatabaseConnectionException e) {
            assertEquals(exception, e.getCause());
        }
    }
}