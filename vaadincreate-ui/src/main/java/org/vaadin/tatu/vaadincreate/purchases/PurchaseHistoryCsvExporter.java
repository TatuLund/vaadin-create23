package org.vaadin.tatu.vaadincreate.purchases;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import org.jspecify.annotations.NullMarked;
import org.vaadin.tatu.vaadincreate.backend.PurchaseService.PurchaseExportRow;

import com.opencsv.CSVWriter;
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
        var bytes = toCsvBytes(rows);
        var fileName = buildFileName(from, to, filePrefix, locale);
        return new StreamResource(() -> new ByteArrayInputStream(bytes),
                fileName);
    }

    byte[] toCsvBytes(List<PurchaseExportRow> rows) {
        var out = new ByteArrayOutputStream();
        try (var writer = new CSVWriter(
                new OutputStreamWriter(out, StandardCharsets.UTF_8))) {
            writer.writeNext(HEADER, false);
            for (var row : rows) {
                writer.writeNext(new String[] { value(row.purchaseId()),
                        value(row.purchaseCreatedAt()),
                        value(row.purchaseStatus()), value(row.requesterName()),
                        value(row.approverName()),
                        value(row.purchaseDecidedAt()),
                        value(row.decisionReason()),
                        value(row.purchaseTotalAmount()),
                        value(row.lineIndex()),
                        value(row.productId()), value(row.productName()),
                        value(row.unitPrice()), value(row.quantity()),
                        value(row.lineTotal()) }, false);
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

    private static String value(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
