package org.vaadin.tatu.vaadincreate.crud;

import java.math.BigDecimal;
import java.util.Locale;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.data.ValueContext;
import com.vaadin.ui.TextField;

public class EuroConverterTest {

    @Test
    public void convertToModelUS() {
        var converter = new EuroConverter();
        var field = new TextField();
        var context = new ValueContext(null, field, Locale.US);
        var string = "10.1 €";
        var result = converter.convertToModel(string, context);
        var target = new BigDecimal("10.1");
        result.ifOk(value -> {
            Assert.assertEquals(0, value.compareTo(target));
        });
        result.ifError(message -> {
            Assert.fail("Cannot convert: " + string);
        });
    }

    @Test
    public void convertToModelFI() {
        var converter = new EuroConverter();
        var field = new TextField();
        var context = new ValueContext(null, field, new Locale("fi"));
        var string = "10,1 €";
        var result = converter.convertToModel(string, context);
        var target = new BigDecimal("10.1");
        result.ifOk(value -> {
            Assert.assertEquals(0, value.compareTo(target));
        });
        result.ifError(message -> {
            Assert.fail("Cannot convert: " + string);
        });
    }

    @Test
    public void convertToModelFails() {
        var converter = new EuroConverter();
        var field = new TextField();
        var context = new ValueContext(null, field, Locale.US);
        var string = "NUMBER";
        var result = converter.convertToModel(string, context);
        result.ifOk(value -> {
            Assert.fail("Converting '" + string + "' should fail.");
        });
        result.ifError(message -> {
            Assert.assertEquals("Cannot convert value to a number", message);
        });
    }

    @Test
    public void convertToPresentationUS() {
        var converter = new EuroConverter();
        var field = new TextField();
        var context = new ValueContext(null, field, Locale.US);
        var number = new BigDecimal("10.1");
        var string = "10.10 €";
        var result = converter.convertToPresentation(number, context);
        Assert.assertEquals(string, result);
    }

    @Test
    public void convertToPresentationFI() {
        var converter = new EuroConverter();
        var field = new TextField();
        var context = new ValueContext(null, field, new Locale("fi"));
        var number = new BigDecimal("10.1");
        var string = "10,10 €";
        var result = converter.convertToPresentation(number, context);
        Assert.assertEquals(string, result);
    }
}
