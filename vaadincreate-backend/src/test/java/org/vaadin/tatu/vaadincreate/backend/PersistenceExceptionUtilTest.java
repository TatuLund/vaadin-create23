package org.vaadin.tatu.vaadincreate.backend;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Constructor;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.SQLException;

import org.junit.Test;

public class PersistenceExceptionUtilTest {

    @Test
    public void detectsReferenceViolationFromSqlIntegrityConstraintException() {
        var exception = new SQLIntegrityConstraintViolationException(
                "FK violation");

        assertTrue(PersistenceExceptionUtil
                .isDeleteReferenceViolation(exception));
    }

    @Test
    public void detectsReferenceViolationFromNestedCauseChain() {
        var rootCause = new SQLIntegrityConstraintViolationException(
                "FK violation");
        var wrapped = new RuntimeException("outer",
                new IllegalStateException("middle", rootCause));

        assertTrue(
                PersistenceExceptionUtil.isDeleteReferenceViolation(wrapped));
    }

    @Test
    public void returnsFalseForNonConstraintException() {
        var exception = new RuntimeException("Some other error");

        assertFalse(PersistenceExceptionUtil
                .isDeleteReferenceViolation(exception));
    }

    @Test
    public void detectsReferenceViolationFromHibernateConstraintException() {
        var exception = createHibernateConstraintViolationException();

        assertTrue(PersistenceExceptionUtil
                .isDeleteReferenceViolation(exception));
    }

    private static Throwable createHibernateConstraintViolationException() {
        try {
            Class<?> exceptionClass = Class
                    .forName(
                            "org.hibernate.exception.ConstraintViolationException");
            for (Constructor<?> constructor : exceptionClass
                    .getConstructors()) {
                Object[] args = new Object[constructor.getParameterCount()];
                Class<?>[] parameterTypes = constructor.getParameterTypes();
                for (int index = 0; index < parameterTypes.length; index++) {
                    args[index] = buildArgument(parameterTypes[index]);
                }
                try {
                    var instance = constructor.newInstance(args);
                    if (instance instanceof Throwable throwable) {
                        return throwable;
                    }
                } catch (ReflectiveOperationException invalidConstructor) {
                    // try next constructor
                }
            }
        } catch (ClassNotFoundException missingHibernateClass) {
            throw new AssertionError(
                    "Hibernate ConstraintViolationException class not found",
                    missingHibernateClass);
        }
        throw new AssertionError(
                "Could not instantiate Hibernate ConstraintViolationException");
    }

    private static Object buildArgument(Class<?> parameterType) {
        if (parameterType == String.class) {
            return "FK violation";
        }
        if (parameterType == SQLException.class) {
            return new SQLIntegrityConstraintViolationException("FK violation");
        }
        if (Throwable.class.isAssignableFrom(parameterType)) {
            return new SQLIntegrityConstraintViolationException("FK violation");
        }
        return null;
    }
}