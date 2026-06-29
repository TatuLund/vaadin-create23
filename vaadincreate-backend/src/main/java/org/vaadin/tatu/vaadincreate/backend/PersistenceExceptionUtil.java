package org.vaadin.tatu.vaadincreate.backend;

import java.sql.SQLIntegrityConstraintViolationException;

import org.hibernate.exception.ConstraintViolationException;
import org.jspecify.annotations.NullMarked;

@NullMarked
public final class PersistenceExceptionUtil {

    private PersistenceExceptionUtil() {
        // utility
    }

    @SuppressWarnings("java:S1872")
    public static boolean isDeleteReferenceViolation(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof SQLIntegrityConstraintViolationException) {
                return true;
            }
            if (current instanceof ConstraintViolationException) {
                return true;
            }
            // Check for PostgreSQL foreign key violation (SQL state 23503)
            // Using reflection to avoid direct dependency on PostgreSQL driver
            if ("org.postgresql.util.PSQLException"
                    .equals(current.getClass().getName())) {
                try {
                    var sqlStateMethod = current.getClass()
                            .getMethod("getSQLState");
                    Object sqlStateObj = sqlStateMethod.invoke(current);
                    if (sqlStateObj instanceof String
                            && "23503".equals(sqlStateObj)) {
                        return true;
                    }
                } catch (ReflectiveOperationException reflectiveFailure) {
                    // ignore
                }
            }
            current = current.getCause();
        }
        return false;
    }
}