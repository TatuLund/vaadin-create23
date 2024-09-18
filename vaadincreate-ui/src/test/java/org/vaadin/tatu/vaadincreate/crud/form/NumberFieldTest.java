package org.vaadin.tatu.vaadincreate.crud.form;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.server.ServiceException;
import com.vaadin.testbench.uiunittest.UIUnitTest;
import com.vaadin.ui.UI;

public class NumberFieldTest extends UIUnitTest {

    private static final String ERROR_MESSAGE = "Cannot convert value to a number.";
    UI ui;
    private NumberField field;

    @Before
    public void setup() throws ServiceException {
        // Vaadin mocks
        ui = mockVaadin();
        field = new NumberField("Field");
        ui.setContent(field);
    }

    @After
    public void cleanup() {
        tearDown();
    }

    @Test
    public void basicTest() {
        field.setValue(20);
        assertEquals("Field", field.getCaption());
        assertEquals(Integer.valueOf(20), field.getValue());
    }

    @Test
    public void setValueByText() {
        test(field.textField).setValue("20");
        assertEquals(Integer.valueOf(20), field.getValue());

        test(field.textField).setValue("-20");
        assertEquals(Integer.valueOf(-20), field.getValue());
    }

    @Test
    public void setValueByTextConversionError() {
        test(field.textField).setValue("2.0");
        assertEquals(ERROR_MESSAGE, test(field.textField).errorMessage());
        assertNull(field.getValue());

        test(field.textField).setValue("2 000");
        assertEquals(ERROR_MESSAGE, test(field.textField).errorMessage());
        assertNull(field.getValue());

        test(field.textField).setValue("abc");
        assertEquals(ERROR_MESSAGE, test(field.textField).errorMessage());
        assertNull(field.getValue());

        test(field.textField).setValue("20");
        assertNull(test(field.textField).errorMessage());
        assertEquals(Integer.valueOf(20), field.getValue());
    }

    @Test
    public void valueChangeEvent() {
        var count = new AtomicInteger(0);
        var user = new AtomicInteger(0);
        field.addValueChangeListener(e -> {
            if (e.isUserOriginated()) {
                user.addAndGet(1);
            }
            count.addAndGet(1);
        });
        field.setValue(20);
        assertEquals(2, count.get());
        assertEquals(0, user.get());

        test(field.textField).setValue("40");
        assertEquals(3, count.get());
        assertEquals(1, user.get());
    }
}
