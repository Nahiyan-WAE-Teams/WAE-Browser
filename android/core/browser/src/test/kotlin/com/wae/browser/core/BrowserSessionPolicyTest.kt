package com.wae.browser.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class BrowserSessionPolicyTest {
    @Test
    fun addressResolverAcceptsHttpAndHttpsOnly() {
        val resolver = AddressBarResolver()

        assertTrue(resolver.resolve("https://example.com") is NavigationTarget.WebUrl)
        assertTrue(resolver.resolve("http://example.com:8080/path") is NavigationTarget.WebUrl)
        assertTrue(resolver.resolve("ftp://example.com") is NavigationTarget.Rejected)
        assertTrue(resolver.resolve("javascript:alert(1)") is NavigationTarget.Rejected)
        assertTrue(resolver.resolve("https://user:pass@example.com") is NavigationTarget.Rejected)
    }

    @Test
    fun addressResolverRejectsInvalidAndZeroPorts() {
        val resolver = AddressBarResolver()

        assertTrue(resolver.resolve("https://example.com:0") is NavigationTarget.Rejected)
        assertTrue(resolver.resolve("https://example.com:65536") is NavigationTarget.Rejected)
        assertTrue(resolver.resolve("https://example.com:443") is NavigationTarget.WebUrl)
    }

    @Test
    fun addressResolverBoundsInputBeforeParsing() {
        val result = AddressBarResolver().resolve("a".repeat(8_193))

        assertEquals(
            NavigationTarget.Rejected("Address exceeds the maximum length"),
            result
        )
    }

    @Test
    fun navigationPolicyBlocksActiveTopLevelSchemesButDelegatesSubframes() {
        val policy = BrowserNavigationPolicy()
        val activeUri = java.net.URI("data:text/html,hello")

        assertTrue(
            policy.evaluate(
                NavigationRequest(activeUri, NavigationSource.LINK, isMainFrame = true)
            ) is NavigationDecision.Block
        )
        assertTrue(
            policy.evaluate(
                NavigationRequest(activeUri, NavigationSource.LINK, isMainFrame = false)
            ) is NavigationDecision.Load
        )
    }

    @Test
    fun navigationPolicyRejectsOverlongTopLevelUrl() {
        val policy = BrowserNavigationPolicy()
        val uri = java.net.URI("https://example.com/" + "a".repeat(8_200))

        assertTrue(
            policy.evaluate(
                NavigationRequest(uri, NavigationSource.LINK, isMainFrame = true)
            ) is NavigationDecision.Block
        )
    }

    @Test
    fun persistedProjectionNeverContainsPrivateTabs() {
        var time = 10L
        val controller = BrowserSessionController(now = { time++ }, newId = { "tab-$time" })
        val standard = controller.createTab(BrowsingMode.STANDARD)
        controller.createTab(BrowsingMode.PRIVATE)

        val persisted = PersistedBrowserSession.from(controller.snapshot())

        assertEquals(listOf(standard.id), persisted.tabs.map { it.id })
        assertNull(persisted.activeTabId)
        assertTrue(persisted.tabs.all { it.mode == BrowsingMode.STANDARD })
    }

    @Test
    fun controllerRejectsOverlongPendingInputAndNavigation() {
        val controller = BrowserSessionController(newId = { "tab-1" })
        val tab = controller.createTab()

        assertFalse(controller.updatePendingInput(tab.id, "x".repeat(8_193)))
        assertFalse(controller.commitNavigation(tab.id, "https://example.com/" + "x".repeat(8_200)))
        assertFalse(controller.commitNavigation(tab.id, "javascript:alert(1)"))
        assertFalse(controller.commitNavigation(tab.id, "https://user:pass@example.com"))
        assertFalse(controller.commitNavigation(tab.id, "https://example.com:0"))
        assertNull(controller.snapshot().activeTab?.currentUrl)
        assertEquals("", controller.snapshot().activeTab?.pendingInput)

        assertTrue(controller.commitNavigation(tab.id, "https://example.com/path"))
        assertEquals("https://example.com/path", controller.snapshot().activeTab?.currentUrl)
        assertEquals("https://example.com/path", controller.snapshot().activeTab?.pendingInput)
    }

    @Test
    fun persistedSessionRejectsPrivateTabsDuplicateIdsAndMissingActiveTab() {
        val standard = BrowserTab(
            id = "standard",
            mode = BrowsingMode.STANDARD,
            title = "Example",
            currentUrl = "https://example.com",
            pendingInput = "https://example.com",
            createdAtEpochMillis = 1L
        )
        val privateTab = standard.copy(id = "private", mode = BrowsingMode.PRIVATE)

        assertThrows(IllegalArgumentException::class.java) {
            PersistedBrowserSession(listOf(privateTab), null)
        }
        assertThrows(IllegalArgumentException::class.java) {
            PersistedBrowserSession(listOf(standard, standard.copy(title = "Duplicate")), "standard")
        }
        assertThrows(IllegalArgumentException::class.java) {
            PersistedBrowserSession(listOf(standard), "missing")
        }
        assertEquals(listOf(standard), PersistedBrowserSession(listOf(standard), "standard").tabs)
    }

    @Test
    fun restoreRejectsInvalidInputWithoutDestroyingCurrentSession() {
        val controller = BrowserSessionController(newId = { "existing" })
        val existing = controller.createTab()
        val standard = BrowserTab(
            id = "restored",
            mode = BrowsingMode.STANDARD,
            title = "Restored",
            currentUrl = "https://example.com",
            pendingInput = "https://example.com",
            createdAtEpochMillis = 2L
        )
        val privateTab = standard.copy(id = "private", mode = BrowsingMode.PRIVATE)

        assertThrows(IllegalArgumentException::class.java) {
            controller.restoreStandardTabs(listOf(privateTab))
        }
        assertThrows(IllegalArgumentException::class.java) {
            controller.restoreStandardTabs(listOf(standard, standard.copy(title = "Duplicate")))
        }
        assertThrows(IllegalArgumentException::class.java) {
            controller.restoreStandardTabs(listOf(standard), "unknown")
        }
        assertThrows(IllegalArgumentException::class.java) {
            controller.restoreStandardTabs(listOf(standard.copy(currentUrl = "javascript:alert(1)")))
        }
        assertThrows(IllegalArgumentException::class.java) {
            controller.restoreStandardTabs((0..50).map { standard.copy(id = "tab-$it") })
        }
        assertEquals(listOf(existing), controller.snapshot().tabs)
        assertEquals(existing.id, controller.snapshot().activeTabId)
    }

    @Test
    fun restoreUsesPreferredStandardTabAndClearsTransientLoadingState() {
        val controller = BrowserSessionController()
        val first = BrowserTab(
            id = "first",
            mode = BrowsingMode.STANDARD,
            title = "First",
            currentUrl = "https://one.example",
            pendingInput = "draft-not-restored",
            isLoading = true,
            progress = 73,
            createdAtEpochMillis = 1L
        )
        val second = first.copy(id = "second", title = "Second", currentUrl = "https://two.example")

        controller.restoreStandardTabs(listOf(first, second), "second")

        val restored = controller.snapshot()
        assertEquals("second", restored.activeTabId)
        assertEquals(listOf("first", "second"), restored.tabs.map { it.id })
        assertTrue(restored.tabs.none { it.isLoading || it.progress != 0 })
        assertEquals("https://one.example", restored.tabs[0].pendingInput)
        assertEquals("https://two.example", restored.tabs[1].pendingInput)
    }
    @Test
    fun persistedProjectionRemovesUnsentAddressDrafts() {
        val controller = BrowserSessionController(newId = { "draft-tab" })
        val tab = controller.createTab()
        assertTrue(controller.commitNavigation(tab.id, "https://example.com"))
        assertTrue(controller.updatePendingInput(tab.id, "unsent search terms"))

        val persisted = PersistedBrowserSession.from(controller.snapshot())

        assertEquals("https://example.com", persisted.tabs.single().pendingInput)
    }

    @Test
    fun navigationPolicyRequiresGestureForExternalActions() {
        val policy = BrowserNavigationPolicy()
        val mail = java.net.URI("mailto:help@example.com")
        val tel = java.net.URI("tel:+15551234567")

        assertTrue(
            policy.evaluate(
                NavigationRequest(mail, NavigationSource.LINK, isMainFrame = true)
            ) is NavigationDecision.Block
        )
        assertTrue(
            policy.evaluate(
                NavigationRequest(mail, NavigationSource.SCRIPT, isMainFrame = true, hasUserGesture = true)
            ) is NavigationDecision.Block
        )
        assertTrue(
            policy.evaluate(
                NavigationRequest(mail, NavigationSource.LINK, isMainFrame = true, hasUserGesture = true)
            ) is NavigationDecision.OpenExternal
        )
        assertTrue(
            policy.evaluate(
                NavigationRequest(tel, NavigationSource.ADDRESS_BAR, isMainFrame = true, hasUserGesture = true)
            ) is NavigationDecision.OpenExternal
        )
    }

    @Test
    fun navigationPolicyRejectsUnsafeHttpDestinations() {
        val policy = BrowserNavigationPolicy()
        val unsafe = listOf(
            java.net.URI("https://user:pass@example.com"),
            java.net.URI("https://example.com:0"),
            java.net.URI("https://example.com:65536")
        )

        unsafe.forEach { uri ->
            assertTrue(
                policy.evaluate(
                    NavigationRequest(uri, NavigationSource.LINK, isMainFrame = true)
                ) is NavigationDecision.Block
            )
        }
        assertTrue(
            policy.evaluate(
                NavigationRequest(
                    java.net.URI("https://example.com:443/path"),
                    NavigationSource.LINK,
                    isMainFrame = true
                )
            ) is NavigationDecision.Load
        )
    }

}
