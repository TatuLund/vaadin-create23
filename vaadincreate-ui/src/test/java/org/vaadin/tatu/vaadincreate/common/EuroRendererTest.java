package org.vaadin.tatu.vaadincreate.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;

import org.junit.Test;

public class EuroRendererTest {

    @Test
    public void ukLocale_usesCommaGroupingAndDotDecimal() {
        NumberFormat format = EuroRenderer.createEuroFormat(Locale.UK);
        assertEuroFormatCommonSettings(format);

        DecimalFormatSymbols symbols = ((DecimalFormat) format)
                .getDecimalFormatSymbols();
        assertEquals(',', symbols.getGroupingSeparator());
        assertEquals('.', symbols.getDecimalSeparator());
    }

    @Test
    public void germanLocale_usesDotGroupingAndCommaDecimal() {
        NumberFormat format = EuroRenderer.createEuroFormat(Locale.GERMANY);
        assertEuroFormatCommonSettings(format);

        DecimalFormatSymbols symbols = ((DecimalFormat) format)
                .getDecimalFormatSymbols();
        assertEquals('.', symbols.getGroupingSeparator());
        assertEquals(',', symbols.getDecimalSeparator());
    }

    @Test
    public void swedishLocale_usesSpaceBasedGroupingAndCommaDecimal() {
        Locale swedish = Locale.of("sv", "SE");
        NumberFormat format = EuroRenderer.createEuroFormat(swedish);
        assertEuroFormatCommonSettings(format);

        DecimalFormatSymbols symbols = ((DecimalFormat) format)
                .getDecimalFormatSymbols();
        char grouping = symbols.getGroupingSeparator();
        assertTrue(grouping == ' ' || grouping == '\u00a0');
        assertEquals(',', symbols.getDecimalSeparator());
    }

    @Test
    public void finnishLocale_usesSpaceBasedGroupingAndCommaDecimal() {
        Locale finnish = Locale.of("fi", "FI");
        NumberFormat format = EuroRenderer.createEuroFormat(finnish);
        assertEuroFormatCommonSettings(format);

        DecimalFormatSymbols symbols = ((DecimalFormat) format)
                .getDecimalFormatSymbols();
        char grouping = symbols.getGroupingSeparator();
        assertTrue(grouping == ' ' || grouping == '\u00a0');
        assertEquals(',', symbols.getDecimalSeparator());
    }

    @Test
    public void formatsValueWithEuroSuffix() {
        NumberFormat format = EuroRenderer.createEuroFormat(Locale.GERMANY);
        String formatted = format.format(1234.5);
        assertTrue("Expected formatted value to end with euro sign",
                formatted.endsWith(" \u20ac"));
    }

    private static void assertEuroFormatCommonSettings(NumberFormat format) {
        assertTrue(format instanceof DecimalFormat);
        DecimalFormat decimalFormat = (DecimalFormat) format;
        assertEquals(2, decimalFormat.getMaximumFractionDigits());
        assertEquals(2, decimalFormat.getMinimumFractionDigits());
        assertEquals(RoundingMode.HALF_UP, decimalFormat.getRoundingMode());
        assertEquals(" \u20ac", decimalFormat.getPositiveSuffix());
        assertEquals(" \u20ac", decimalFormat.getNegativeSuffix());
    }
}
