package org.vaadin.tatu.vaadincreate.purchases;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.vaadin.tatu.vaadincreate.backend.PurchaseService.PurchaseExportRow;

import com.opencsv.CSVWriter;
import com.opencsv.ICSVWriter;
import com.vaadin.server.StreamResource;

@NullMarked
@SuppressWarnings("serial")
public class PurchaseHistoryCsvExporter implements Serializable {

    private static final String[] HEADER = { "purchase_id",
            "purchase_created_at", "purchase_status", "requester_name",
            "approver_name", "purchase_decided_at", "decision_reason",
            "purchase_total_amount", "line_index", "product_id", "product_name",
            "unit_price", "quantity", "line_total" };

    public StreamResource createResource(LocalDate from, LocalDate to,
            List<PurchaseExportRow> rows, Locale locale, String filePrefix) {
        var bytes = toCsvBytes(rows, locale);
        var fileName = buildFileName(from, to, filePrefix, locale);
        return new StreamResource(() -> new ByteArrayInputStream(bytes),
                fileName);
    }

    byte[] toCsvBytes(List<PurchaseExportRow> rows) {
        return toCsvBytes(rows, Locale.ROOT);
    }

    byte[] toCsvBytes(List<PurchaseExportRow> rows, Locale locale) {
        var out = new ByteArrayOutputStream();
        var separator = separatorFor(locale);
        try (var writer = new CSVWriter(
                new OutputStreamWriter(out, StandardCharsets.UTF_8),
                separator,
                ICSVWriter.DEFAULT_QUOTE_CHARACTER,
                ICSVWriter.DEFAULT_ESCAPE_CHARACTER,
                ICSVWriter.DEFAULT_LINE_END)) {
            writer.writeNext(HEADER, false);
            for (var row : rows) {
                writer.writeNext(new String[] { value(row.purchaseId(), locale),
                        value(row.purchaseCreatedAt(), locale),
                        value(row.purchaseStatus(), locale),
                        value(row.requesterName(), locale),
                        value(row.approverName(), locale),
                        value(row.purchaseDecidedAt(), locale),
                        value(row.decisionReason(), locale),
                        value(row.purchaseTotalAmount(), locale),
                        value(row.lineIndex(), locale),
                        value(row.productId(), locale),
                        value(row.productName(), locale),
                        value(row.unitPrice(), locale),
                        value(row.quantity(), locale),
                        value(row.lineTotal(), locale) }, false);
            }
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Failed to build CSV export", e);
        }
        return out.toByteArray();
    }

    private static String buildFileName(LocalDate from, LocalDate to,
            String filePrefix, Locale locale) {
        var tokenFormat = DateTimeFormatter.ofPattern("MMddyy", locale);
        var sanitizedPrefix = filePrefix.toLowerCase(locale)
                .replaceAll("[^\\p{Alnum}]+", "-").replaceAll("(^-|-$)", "");
        if (sanitizedPrefix.isEmpty()) {
            sanitizedPrefix = "export";
        }
        return String.format("%s-%s-%s.csv", sanitizedPrefix,
                from.format(tokenFormat), to.format(tokenFormat));
    }

    private static String value(@Nullable Object value, Locale locale) {
        if (value == null) {
            return "";
        }
        if (value instanceof BigDecimal decimal) {
            var decimalSeparator = DecimalFormatSymbols.getInstance(locale)
                    .getDecimalSeparator();
            return decimalSeparator == '.' ? decimal.toPlainString()
                    : decimal.toPlainString().replace('.', decimalSeparator);
        }
        return String.valueOf(value);
    }

    private static char separatorFor(Locale locale) {
        var decimalSeparator = DecimalFormatSymbols.getInstance(locale)
                .getDecimalSeparator();
        return decimalSeparator == ',' ? ';' : ',';
    }
}
