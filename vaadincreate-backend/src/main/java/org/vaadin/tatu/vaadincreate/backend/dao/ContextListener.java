package org.vaadin.tatu.vaadincreate.backend.dao;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

@WebListener
public class ContextListener implements ServletContextListener {
    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        // Close the Hibernate SessionFactory, Hikari DataSource, etc.
        HibernateUtil.shutdown();
    }
}
