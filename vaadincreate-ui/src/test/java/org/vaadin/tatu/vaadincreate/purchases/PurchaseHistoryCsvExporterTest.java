package org.vaadin.tatu.vaadincreate.purchases;

import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

import org.junit.Test;
import org.vaadin.tatu.vaadincreate.backend.PurchaseService.PurchaseExportRow;

public class PurchaseHistoryCsvExporterTest {

    @Test
    public void should_build_csv_with_header_and_escaped_values() {
        var exporter = new PurchaseHistoryCsvExporter();
        var rows = List.of(new PurchaseExportRow(101,
                Instant.parse("2025-02-01T10:15:30Z"), "PENDING", "Alice",
                "Boss", null, "Needs, \"quoted\" text\nline",
                new BigDecimal("99.90"), 1, 55, "Test Product",
                new BigDecimal("9.99"), 10, new BigDecimal("99.90")));

        var bytes = exporter.toCsvBytes(rows);
        var csv = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);

        assertTrue(csv.startsWith(
                "purchase_id,purchase_created_at,purchase_status,requester_name"));
        assertTrue(csv.contains("\"Needs, \"\"quoted\"\" text\nline\""));
    }

    @Test
    public void should_build_export_file_name_with_date_tokens() {
        var exporter = new PurchaseHistoryCsvExporter();
        var resource = exporter.createResource(LocalDate.of(2025, 1, 2),
                LocalDate.of(2025, 3, 5), List.of(), Locale.ENGLISH, "Export");
        assertTrue(resource.getFilename().startsWith("export-010225-030525"));
    }
}
