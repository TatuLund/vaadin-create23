package org.vaadin.tatu.vaadincreate;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.vaadin.tatu.vaadincreate.auth.CurrentUser;
import org.vaadin.tatu.vaadincreate.backend.data.User;

import java.io.IOException;

@NullMarked
@WebFilter(urlPatterns = "/*", asyncSupported = true)
public class LoggingFilter implements Filter {

    private static final Logger logger = LoggerFactory
            .getLogger(LoggingFilter.class);

    private static boolean isGoodUrl(HttpServletRequest request) {
        // Check if the URL is a VAADIN or valid application endpoint,
        // considering context path
        String url = request.getRequestURI();
        String contextPath = request.getContextPath();
        return url.equals(contextPath + "/")
                || url.startsWith(contextPath + "/VAADIN")
                || url.startsWith(contextPath + "/UIDL")
                || url.startsWith(contextPath + "/HEARTBEAT")
                || url.startsWith(contextPath + "/PUSH")
                || url.startsWith(contextPath + "/APP");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
            FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpSession session = httpRequest.getSession(false);
        // Check if the URL is a VAADIN request or a valid application URL
        if (!isGoodUrl(httpRequest)) {
            getHttpResponse(response, httpRequest);
            return;
        }
        if (session != null) {
            populateUserDetails(session);
        }

        // Pass the request along the filter chain
        chain.doFilter(request, response);
    }

    private static void getHttpResponse(ServletResponse response,
            HttpServletRequest httpRequest) throws IOException {
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        httpResponse.setContentType("text/plain");
        httpResponse.getWriter()
                .write("Invalid request url: " + httpRequest.getRequestURI());
        logger.error("Invalid request url: {}", httpRequest.getRequestURI());
        // Return 404
        httpResponse.setStatus(404);
    }

    @SuppressWarnings("null")
    private static void populateUserDetails(HttpSession session) {
        // Add user id to log messages if the user is logged in
        var user = (User) session
                .getAttribute(CurrentUser.CURRENT_USER_SESSION_ATTRIBUTE_KEY);
        if (user != null) {
            var userId = String.format("[%s/%s]", user.getRole().toString(),
                    user.getName());
            MDC.put("userId", userId);
        }
    }

}
