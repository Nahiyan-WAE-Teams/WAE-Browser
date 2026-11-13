package com.wae.browser.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BookmarkPolicyTest {
    @Test
    fun bookmarkTitleRemovesControlAndBidiOverrideCharacters() {
        val normalized = normalizeBookmarkTitle("  Safe\u202Etxt\u0000 title  ", "fallback.example")
        assertEquals("Safetxt title", normalized)
        assertTrue(isSafeBookmarkTitle(normalized))
    }

    @Test
    fun blankOrInvisibleBookmarkTitleUsesHostFallback() {
        assertEquals("example.com", normalizeBookmarkTitle("\u202E\u0000 ", "example.com"))
    }

    @Test
    fun storedBookmarkTitlesRejectInvisibleControlCharacters() {
        assertFalse(isSafeBookmarkTitle("visible\u202Ehidden"))
        assertFalse(isSafeBookmarkTitle("line\nbreak"))
        assertTrue(isSafeBookmarkTitle("日本語 bookmark"))
    }

    @Test
    fun bookmarkIdsRejectControlCharactersAndUnboundedValues() {
        assertTrue(isSafeBookmarkId("bookmark-123"))
        assertFalse(isSafeBookmarkId("bookmark\n123"))
        assertFalse(isSafeBookmarkId("x".repeat(129)))
    }

    @Test
    fun bookmarkTitleLengthIsBoundedAfterNormalization() {
        assertEquals(512, normalizeBookmarkTitle("x".repeat(600), "fallback").length)
    }
}
