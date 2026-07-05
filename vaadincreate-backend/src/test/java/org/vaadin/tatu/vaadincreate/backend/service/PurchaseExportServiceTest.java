package org.vaadin.tatu.vaadincreate.backend.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import org.junit.Before;
import org.junit.Test;
import org.vaadin.tatu.vaadincreate.backend.PurchaseService;

@SuppressWarnings("null")
public class PurchaseExportServiceTest {

    private PurchaseService purchaseService;

    @Before
    public void setUp() {
        UserServiceImpl.getInstance();
        ProductDataServiceImpl.getInstance();
        purchaseService = PurchaseServiceImpl.getInstance();
    }

    @Test
    public void should_fetch_flattened_export_rows_for_range() {
        Instant from = ZonedDateTime.now(ZoneOffset.UTC).minusMonths(3)
                .toInstant();
        Instant to = ZonedDateTime.now(ZoneOffset.UTC).plusDays(1).toInstant();

        var rows = purchaseService.fetchPurchaseExportRows(from, to);
        assertFalse(rows.isEmpty());
        var row = rows.get(0);
        assertNotNull(row.purchaseId());
        assertNotNull(row.productId());
        assertNotNull(row.purchaseCreatedAt());
        assertNotNull(row.productName());
        assertEquals(1, row.lineIndex());
    }

    @Test
    public void should_resolve_first_matching_row_index() {
        Instant from = ZonedDateTime.now(ZoneOffset.UTC).minusMonths(1)
                .toInstant();
        var index = purchaseService.resolveFirstMatchingRowIndex(from);
        assertTrue(index == null || index >= 0);
    }
}
