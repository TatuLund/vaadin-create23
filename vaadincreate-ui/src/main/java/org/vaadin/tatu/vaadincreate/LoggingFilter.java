package org.vaadin.tatu.vaadincreate;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.jspecify.annotations.NullMarked;
import org.slf4j.MDC;
import org.vaadin.tatu.vaadincreate.auth.CurrentUser;
import org.vaadin.tatu.vaadincreate.backend.data.User;

import java.io.IOException;

@NullMarked
@WebFilter("/*")
public class LoggingFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
            FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpSession session = httpRequest.getSession(false);
        if (session != null) {
            // Add user id to log messages if the user is logged in
            var user = (User) session.getAttribute(
                    CurrentUser.CURRENT_USER_SESSION_ATTRIBUTE_KEY);
            if (user != null) {
                var userId = String.format("[%s/%s]", user.getRole().toString(),
                        user.getName());
                MDC.put("userId", userId);
            }
        }
        // Pass the request along the filter chain
        chain.doFilter(request, response);
    }

}
