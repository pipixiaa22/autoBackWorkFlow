package com.ckrey.autobackworkflow.common.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ckrey.autobackworkflow.common.exception.BizException;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Test;

class CursorPageTests {
    @Test
    void exposesOnlyRequestedItemsAndProducesCursorForTheLastReturnedItem() {
        List<Entry> records = List.of(new Entry(3L, new Date(3_000)), new Entry(2L, new Date(2_000)),
                new Entry(1L, new Date(1_000)));

        CursorPage<Entry> page = CursorPage.from(records, 2, Entry::createdAt, Entry::id);

        assertEquals(List.of(3L, 2L), page.items().stream().map(Entry::id).toList());
        assertTrue(page.hasMore());
        assertNotNull(page.nextCursor());
        CursorPage.Cursor cursor = CursorPage.decode(page.nextCursor());
        assertEquals(2L, cursor.id());
        assertEquals(2_000, cursor.createdAt().getTime());
    }

    @Test
    void rejectsMalformedCursorsAndDoesNotCreateCursorForFinalPage() {
        CursorPage<Entry> page = CursorPage.from(List.of(new Entry(1L, new Date(1_000))), 20,
                Entry::createdAt, Entry::id);

        assertFalse(page.hasMore());
        assertEquals(null, page.nextCursor());
        BizException exception = assertThrows(BizException.class, () -> CursorPage.decode("invalid"));
        assertEquals("COMMON_INVALID_CURSOR", exception.getCode());
    }

    private record Entry(Long id, Date createdAt) { }
}
