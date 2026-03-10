package org.vaadin.tatu.vaadincreate;

import java.io.IOException;
import java.io.InputStream;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Serves PWA static resources with explicit MIME types.
 */
@WebServlet(urlPatterns = { "/sw.js", "/manifest.webmanifest", "/offline.html",
        "/icons/*" })
@SuppressWarnings("serial")
public class PwaResourcesServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {
        String resourcePath = resolveResourcePath(request);
        if (resourcePath == null || resourcePath.contains("..")) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        try (InputStream resourceStream = getServletContext()
                .getResourceAsStream(resourcePath)) {
            if (resourceStream == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            response.setContentType(resolveContentType(resourcePath));
            if (resourcePath.endsWith("/sw.js")) {
                // SW updates should be checked frequently.
                response.setHeader("Cache-Control", "no-cache");
            }
            resourceStream.transferTo(response.getOutputStream());
        }
    }

    private String resolveResourcePath(HttpServletRequest request) {
        String servletPath = request.getServletPath();
        if ("/icons".equals(servletPath)) {
            String pathInfo = request.getPathInfo();
            if (pathInfo == null) {
                return null;
            }
            return servletPath + pathInfo;
        }
        return servletPath;
    }

    private String resolveContentType(String resourcePath) {
        if (resourcePath.endsWith(".js")) {
            return "application/javascript";
        }
        if (resourcePath.endsWith(".webmanifest")) {
            return "application/manifest+json";
        }
        if (resourcePath.endsWith(".png")) {
            return "image/png";
        }
        if (resourcePath.endsWith(".html")) {
            return "text/html;charset=UTF-8";
        }
        return "application/octet-stream";
    }
}
