package org.vaadin.tatu.vaadincreate.crud.form;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Locale;

import org.junit.Before;
import org.junit.Test;

import com.vaadin.data.ValueContext;
import com.vaadin.ui.TextField;

public class StockCountConverterTest {

    private static final String ERROR_MESSAGE = "Invalid stock count";
    private static final char MINUS = '\u002D';
    private StockCountConverter converter;
    private TextField field;
    private ValueContext context;

    @Before
    public void setup() {
        Locale.setDefault(Locale.ENGLISH);
        converter = new StockCountConverter(ERROR_MESSAGE);

        field = new TextField();
        context = new ValueContext(null, field, Locale.ENGLISH);
    }

    @Test
    public void testConvertToModel() {

        // Test with valid stock count
        var result = converter.convertToModel("10", context);
        assertNotNull(result);
        assertFalse(result.isError());
        result.ifOk(value -> assertEquals(Integer.valueOf(10), value));

        // Test with negative stock count
        result = converter.convertToModel(MINUS + "10", context);
        assertNotNull(result);
        assertFalse(result.isError());
        result.ifOk(value -> assertEquals(Integer.valueOf(-10), value));

        // Test with null stock count
        result = converter.convertToModel(null, context);
        assertNotNull(result);
        assertFalse(result.isError());
        result.ifOk(value -> assertEquals(Integer.valueOf(0), value));

        // Test with invalid stock count
        result = converter.convertToModel("2.2", context);
        assertNotNull(result);
        assertTrue(result.isError());
        result.ifError(error -> assertEquals(ERROR_MESSAGE, error));

        // Test with invalid stock count
        result = converter.convertToModel("abc", context);
        assertNotNull(result);
        assertTrue(result.isError());
        result.ifError(error -> assertEquals(ERROR_MESSAGE, error));

        // Test with invalid stock count
        result = converter.convertToModel("10 000", context);
        assertNotNull(result);
        assertTrue(result.isError());
        result.ifError(error -> assertEquals(ERROR_MESSAGE, error));
    }

    @Test
    public void testConvertToPresentation() {
        var result = converter.convertToPresentation(1000, context);
        assertEquals("1000", result);

        result = converter.convertToPresentation(-1000, context);
        assertEquals(MINUS + "1000", result);

        result = converter.convertToPresentation(null, context);
        assertNull(result);
    }
}