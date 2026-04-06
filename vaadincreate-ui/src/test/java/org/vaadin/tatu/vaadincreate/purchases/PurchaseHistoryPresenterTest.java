package org.vaadin.tatu.vaadincreate.purchases;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;
import org.vaadin.tatu.vaadincreate.AbstractUITest;
import org.vaadin.tatu.vaadincreate.VaadinCreateUI;
import org.vaadin.tatu.vaadincreate.backend.PurchaseService;

import com.vaadin.server.ServiceException;

public class PurchaseHistoryPresenterTest extends AbstractUITest {

    private VaadinCreateUI ui;

    @Before
    public void setup() throws ServiceException {
        ui = new VaadinCreateUI();
        mockVaadin(ui);
    }

    @After
    public void cleanup() {
        if (ui.getAccessControl().isUserSignedIn()) {
            logout();
        }
        tearDown();
    }

    @Test
    public void startExport_throws_whenCurrentUserIsNotAdmin() {
        // GIVEN: A non-admin user is logged in
        login("User1", "user1");
        var presenter = new PurchaseHistoryPresenter();

        // WHEN: Starting export
        ThrowingRunnable runnable = () -> presenter.startExport(
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2025, 1, 2), rows -> {
                }, throwable -> {
                });

        // THEN: An exception is thrown
        assertThrows(IllegalStateException.class,
                runnable);
    }

    @Test
    public void startExport_throws_whenRangeExceedsThreeMonths() {
        login();
        var presenter = new PurchaseHistoryPresenter();

        // WHEN: Starting export with a range of 4 months
        ThrowingRunnable runnable = () -> presenter.startExport(
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2025, 4, 2), rows -> {
                }, throwable -> {
                });

        // THEN: An exception is thrown
        assertThrows(IllegalArgumentException.class,
                runnable);
    }

    @Test
    public void startExport_throws_toDateBeforeFromDate() {
        login();
        var presenter = new PurchaseHistoryPresenter();

        // WHEN: Starting export with a range where toDate is before fromDate
        ThrowingRunnable runnable = () -> presenter.startExport(
                LocalDate.of(2025, 4, 2),
                LocalDate.of(2025, 1, 1), rows -> {
                }, throwable -> {
                });

        // THEN: An exception is thrown
        assertThrows(IllegalArgumentException.class,
                runnable);
    }

    @Test
    public void startExport_runsFailureCallback_whenFetchingFails()
            throws ReflectiveOperationException {
        login();

        // GIVEN: A presenter with a purchase service that throws when fetching
        // export rows
        var presenter = createPurchaseHistoryPresenterWithFailingService();

        var successCalled = new AtomicBoolean(false);
        var failureCalled = new AtomicBoolean(false);
        var failureThrowable = new AtomicReference<Throwable>();

        // WHEN: Starting export
        var future = presenter.startExport(LocalDate.of(2025, 1, 1),
                LocalDate.of(2025, 1, 2), rows -> {
                    successCalled.set(true);
                }, throwable -> {
                    failureCalled.set(true);
                    failureThrowable.set(throwable);
                });

        future.join();

        // THEN: Failure callback is called with the exception and success
        // callback is not called
        assertFalse(successCalled.get());
        assertTrue(failureCalled.get());
        assertNotNull(failureThrowable.get());
    }

    private PurchaseHistoryPresenter createPurchaseHistoryPresenterWithFailingService()
            throws ReflectiveOperationException {
        var presenter = new PurchaseHistoryPresenter();
        var failingPurchaseService = getFailingPurchaseService();
        setPrivateField(presenter, "purchaseService", failingPurchaseService);
        setPrivateField(presenter, "executor", new DirectExecutorService());
        return presenter;
    }

    private PurchaseService getFailingPurchaseService() {
        return (PurchaseService) Proxy
                .newProxyInstance(PurchaseService.class.getClassLoader(),
                        new Class[] { PurchaseService.class },
                        (proxy, method, args) -> {
                            if ("fetchPurchaseExportRows"
                                    .equals(method.getName())) {
                                throw new RuntimeException("boom");
                            }
                            throw new UnsupportedOperationException(
                                    "Unexpected method call: " + method
                                            .getName());
                        });
    }

    private static void setPrivateField(Object target, String fieldName,
            Object value) throws ReflectiveOperationException {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static final class DirectExecutorService
            extends AbstractExecutorService {

        private volatile boolean shutdown;

        @Override
        public void shutdown() {
            shutdown = true;
        }

        @Override
        public List<Runnable> shutdownNow() {
            shutdown = true;
            return List.of();
        }

        @Override
        public boolean isShutdown() {
            return shutdown;
        }

        @Override
        public boolean isTerminated() {
            return isShutdown();
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) {
            return true;
        }

        @Override
        public void execute(Runnable command) {
            command.run();
        }
    }
}
