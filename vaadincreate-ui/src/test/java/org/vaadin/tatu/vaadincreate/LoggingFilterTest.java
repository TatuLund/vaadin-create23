package org.vaadin.tatu.vaadincreate;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.MDC;
import org.vaadin.tatu.vaadincreate.auth.CurrentUser;
import org.vaadin.tatu.vaadincreate.backend.data.User;
import org.vaadin.tatu.vaadincreate.backend.data.User.Role;

import com.vaadin.server.ServiceException;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinResponse;
import com.vaadin.server.VaadinService;
import com.vaadin.server.VaadinServletRequest;
import com.vaadin.server.VaadinServletResponse;
import com.vaadin.server.VaadinServletService;
import com.vaadin.testbench.uiunittest.UIUnitTest;
import com.vaadin.ui.UI;

public class LoggingFilterTest extends UIUnitTest {

    private UI ui;

    @Before
    public void setup() throws ServiceException {
        // Vaadin mocks
        ui = mockVaadin();
        var user = new User();
        user.setName("Mock");
        user.setRole(Role.ADMIN);
        ui.getSession().getSession().setAttribute(
                CurrentUser.CURRENT_USER_SESSION_ATTRIBUTE_KEY, user);
    }

    @After
    public void cleanup() {
        tearDown();
    }

    @Test
    public void testLoggingFilter() {
        AtomicInteger counter = new AtomicInteger(0);
        FilterChain chain = new FilterChain() {
            @Override
            public void doFilter(ServletRequest request,
                    ServletResponse response) {
                counter.incrementAndGet();
            }
        };
        var filter = new LoggingFilter();
        try {
            filter.doFilter((HttpServletRequest) VaadinRequest.getCurrent(),
                    (HttpServletResponse) VaadinResponse.getCurrent(), chain);
        } catch (IOException | ServletException e) {
            // Handle exception
        }
        assertEquals(1, counter.get());
        assertEquals("[ADMIN/Mock]", MDC.get("userId"));
    }

    @Test
    public void testBadUrl() {
        AtomicInteger counter = new AtomicInteger(0);
        FilterChain chain = new FilterChain() {
            @Override
            public void doFilter(ServletRequest request,
                    ServletResponse response) {
                counter.incrementAndGet();
            }
        };
        var filter = new LoggingFilter();
        var request = new MockRequest(
                (HttpServletRequest) VaadinRequest.getCurrent(),
                (VaadinServletService) VaadinService.getCurrent(), "/wrong");
        var response = new MockResponse(
                (HttpServletResponse) VaadinResponse.getCurrent(),
                (VaadinServletService) VaadinService.getCurrent());
        try {
            filter.doFilter(request, response, chain);
        } catch (IOException | ServletException e) {
            // Handle exception
        }
        assertEquals(404, response.getStatus());
        assertEquals(0, counter.get());
    }

    @Test
    public void testGoodUrl() {
        var goodUrls = Set.of("VAADIN", "APP", "UIDL", "HEARTBEAT", "PUSH");
        goodUrls.forEach(this::testGoodUrl);
    }

    private void testGoodUrl(String url) {
        AtomicInteger counter = new AtomicInteger(0);
        FilterChain chain = new FilterChain() {
            @Override
            public void doFilter(ServletRequest request,
                    ServletResponse response) {
                counter.incrementAndGet();
            }
        };
        var filter = new LoggingFilter();
        var contextPath = VaadinRequest.getCurrent().getContextPath();
        var request = new MockRequest(
                (HttpServletRequest) VaadinRequest.getCurrent(),
                (VaadinServletService) VaadinService.getCurrent(),
                contextPath + "/" + url + "/something");
        var response = new MockResponse(
                (HttpServletResponse) VaadinResponse.getCurrent(),
                (VaadinServletService) VaadinService.getCurrent());
        try {
            filter.doFilter(request, response, chain);
        } catch (IOException | ServletException e) {
            // Handle exception
        }
        assertEquals(200, response.getStatus());
        assertEquals(1, counter.get());
    }

    public static class MockRequest extends VaadinServletRequest {

        private final String requestURI;

        public MockRequest(HttpServletRequest request,
                VaadinServletService vaadinService, String requestURI) {
            super(request, vaadinService);
            this.requestURI = requestURI;
        }

        @Override
        public String getRequestURI() {
            return requestURI;
        }
    }

    public static class MockResponse extends VaadinServletResponse {

        public MockResponse(HttpServletResponse response,
                VaadinServletService vaadinService) {
            super(response, vaadinService);
        }

        @Override
        public PrintWriter getWriter() {
            return new java.io.PrintWriter(System.out);
        }
    }
}
