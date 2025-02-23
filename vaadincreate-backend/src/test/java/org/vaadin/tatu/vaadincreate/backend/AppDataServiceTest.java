package org.vaadin.tatu.vaadincreate.backend;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import org.vaadin.tatu.vaadincreate.backend.service.AppDataServiceImpl;

/**
 * Test class for {@link AppDataService}.
 *
 * These integration tests run against the in-memory H2 database.
 */
public class AppDataServiceTest {

    private AppDataService service;

    @Before
    public void setUp() {
        service = AppDataServiceImpl.getInstance();
    }

    @Test
    public void createNewMessage() {
        service.updateMessage("My Test Message");
        var message = service.getMessage();
        assertEquals("My Test Message", message.getMessage());
    }

}
