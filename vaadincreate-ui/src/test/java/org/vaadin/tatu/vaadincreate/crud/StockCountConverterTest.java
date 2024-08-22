package org.vaadin.tatu.vaadincreate.crud;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Locale;

import org.junit.Test;
import com.vaadin.data.ValueContext;
import com.vaadin.ui.TextField;

public class StockCountConverterTest {

    @Test
    public void testConvertToModel() {
        var converter = new StockCountConverter("Invalid stock count");

        // Test with valid stock count
        var result = converter.convertToModel("10", new ValueContext());
        assertNotNull(result);
        assertFalse(result.isError());
        result.ifOk(value -> assertEquals(Integer.valueOf(10), value));

        // Test with null stock count
        result = converter.convertToModel(null, new ValueContext());
        assertNotNull(result);
        assertFalse(result.isError());
        result.ifOk(value -> assertEquals(Integer.valueOf(0), value));

        // Test with invalid stock count
        result = converter.convertToModel("abc", new ValueContext());
        assertNotNull(result);
        assertTrue(result.isError());
        result.ifError(error -> assertEquals("Invalid stock count", error));

        // Test with invalid stock count
        result = converter.convertToModel("10 000", new ValueContext());
        assertNotNull(result);
        assertTrue(result.isError());
        result.ifError(error -> assertEquals("Invalid stock count", error));
    }

    @Test
    public void testConvertToPresentation() {
        var converter = new StockCountConverter("Invalid stock count");
        var field = new TextField();
        var context = new ValueContext(null, field, new Locale("fi"));

        var result = converter.convertToPresentation(1000, context);
        assertEquals("1000", result);
    }
}