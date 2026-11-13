package com.wae.browser.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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
        assertEquals(standard.id, persisted.activeTabId)
        assertTrue(persisted.tabs.all { it.mode == BrowsingMode.STANDARD })
    }

    @Test
    fun controllerRejectsOverlongPendingInputAndNavigation() {
        val controller = BrowserSessionController(newId = { "tab-1" })
        val tab = controller.createTab()

        assertFalse(controller.updatePendingInput(tab.id, "x".repeat(8_193)))
        assertFalse(controller.commitNavigation(tab.id, "https://example.com/" + "x".repeat(8_200)))
        assertEquals("", controller.snapshot().activeTab?.pendingInput)
        assertEquals(null, controller.snapshot().activeTab?.currentUrl)
    }
}
