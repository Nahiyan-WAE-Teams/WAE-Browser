package com.wae.browser.core

import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.UUID

enum class BrowsingMode { STANDARD, PRIVATE }

enum class NavigationSecurity { UNKNOWN, SECURE, INSECURE, CERTIFICATE_ERROR }

data class BrowserTab(
    val id: String,
    val mode: BrowsingMode,
    val title: String,
    val currentUrl: String?,
    val pendingInput: String,
    val isLoading: Boolean = false,
    val progress: Int = 0,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val security: NavigationSecurity = NavigationSecurity.UNKNOWN,
    val createdAtEpochMillis: Long
) {
    init {
        require(id.isNotBlank())
        require(progress in 0..100)
        require(title.length <= 512)
    }
}

data class BrowserSession(
    val tabs: List<BrowserTab>,
    val activeTabId: String?
) {
    init {
        require(tabs.map(BrowserTab::id).distinct().size == tabs.size) {
            "Tab IDs must be unique"
        }
        require((tabs.isEmpty() && activeTabId == null) ||
            tabs.any { it.id == activeTabId }) {
            "Active tab must refer to an existing tab"
        }
    }

    val activeTab: BrowserTab?
        get() = tabs.firstOrNull { it.id == activeTabId }

    /** Only ordinary tabs are eligible for durable session storage. */
    fun persistableTabs(): List<BrowserTab> =
        tabs.filter { it.mode == BrowsingMode.STANDARD }
}

sealed interface NavigationTarget {
    data class WebUrl(val uri: URI) : NavigationTarget
    data class SearchQuery(val query: String, val uri: URI) : NavigationTarget
    data class Rejected(val reason: String) : NavigationTarget
}

enum class SearchProvider(val endpoint: String) {
    DUCKDUCKGO("https://duckduckgo.com/"),
    GOOGLE("https://www.google.com/search"),
    BRAVE("https://search.brave.com/search")
}

/**
 * Converts address-bar input into an explicit HTTP(S) navigation or a search.
 * Active content and local-resource schemes are never accepted as top-level
 * address-bar destinations.
 */
class AddressBarResolver(
    private val searchProvider: SearchProvider = SearchProvider.DUCKDUCKGO
) {
    fun resolve(rawInput: String): NavigationTarget {
        val input = rawInput.trim()
        if (input.isEmpty()) return NavigationTarget.Rejected("Enter a search or web address")
        if (input.any { it.isISOControl() }) {
            return NavigationTarget.Rejected("Address contains control characters")
        }

        val hostLike = looksLikeHost(input)
        val explicitScheme = SCHEME_PATTERN.containsMatchIn(input)
        val candidate = when {
            hostLike -> "https://$input"
            explicitScheme -> input
            else -> return search(input)
        }

        val parsed = try {
            URI(candidate).normalize()
        } catch (_: Exception) {
            return NavigationTarget.Rejected("Address is not valid")
        }

        val scheme = parsed.scheme?.lowercase()
        if (scheme != "http" && scheme != "https") {
            return NavigationTarget.Rejected("Only HTTP and HTTPS addresses are supported")
        }
        if (parsed.host.isNullOrBlank() || parsed.rawUserInfo != null) {
            return NavigationTarget.Rejected("Address must contain a valid host and no embedded credentials")
        }
        if (parsed.port !in -1..65535) {
            return NavigationTarget.Rejected("Address contains an invalid port")
        }
        return NavigationTarget.WebUrl(parsed)
    }

    private fun search(query: String): NavigationTarget {
        val encoded = URLEncoder.encode(query, StandardCharsets.UTF_8)
        val uri = when (searchProvider) {
            SearchProvider.DUCKDUCKGO -> URI("${searchProvider.endpoint}?q=$encoded")
            SearchProvider.GOOGLE, SearchProvider.BRAVE ->
                URI("${searchProvider.endpoint}?q=$encoded")
        }
        return NavigationTarget.SearchQuery(query, uri)
    }

    private fun looksLikeHost(value: String): Boolean {
        if (value.any(Char::isWhitespace) || value.contains('/') || value.contains('?')) return false
        val hostPart = value.substringBefore(':')
        return hostPart == "localhost" || hostPart.contains('.')
    }

    private companion object {
        val SCHEME_PATTERN = Regex("^[A-Za-z][A-Za-z0-9+.-]*:")
    }
}

/**
 * In-memory session coordinator. Persistence is deliberately delegated to a
 * repository so private tabs can never accidentally be serialized by default.
 */
class BrowserSessionController(
    private val now: () -> Long = System::currentTimeMillis,
    private val newId: () -> String = { UUID.randomUUID().toString() }
) {
    private val tabs = mutableListOf<BrowserTab>()
    private var activeId: String? = null

    @Synchronized
    fun snapshot(): BrowserSession = BrowserSession(tabs.toList(), activeId)

    @Synchronized
    fun createTab(mode: BrowsingMode = BrowsingMode.STANDARD): BrowserTab {
        val tab = BrowserTab(
            id = newId(),
            mode = mode,
            title = "New tab",
            currentUrl = null,
            pendingInput = "",
            createdAtEpochMillis = now()
        )
        require(tabs.none { it.id == tab.id }) { "Generated tab ID already exists" }
        tabs += tab
        activeId = tab.id
        return tab
    }

    @Synchronized
    fun selectTab(id: String): Boolean {
        if (tabs.none { it.id == id }) return false
        activeId = id
        return true
    }

    @Synchronized
    fun closeTab(id: String): BrowserTab? {
        val index = tabs.indexOfFirst { it.id == id }
        if (index < 0) return null
        val removed = tabs.removeAt(index)
        if (activeId == id) {
            activeId = tabs.getOrNull(index.coerceAtMost(tabs.lastIndex))?.id
        }
        return removed
    }

    @Synchronized
    fun updatePendingInput(id: String, value: String): Boolean =
        update(id) { it.copy(pendingInput = value) }

    @Synchronized
    fun commitNavigation(id: String, url: String): Boolean =
        update(id) {
            it.copy(
                currentUrl = url,
                pendingInput = url,
                title = url,
                isLoading = true,
                progress = 0,
                canGoBack = it.currentUrl != null,
                canGoForward = false,
                security = NavigationSecurity.UNKNOWN
            )
        }

    @Synchronized
    fun updatePageState(
        id: String,
        title: String?,
        isLoading: Boolean,
        progress: Int,
        canGoBack: Boolean,
        canGoForward: Boolean,
        security: NavigationSecurity
    ): Boolean {
        require(progress in 0..100)
        return update(id) {
            it.copy(
                title = title?.take(512)?.ifBlank { null } ?: it.title,
                isLoading = isLoading,
                progress = progress,
                canGoBack = canGoBack,
                canGoForward = canGoForward,
                security = security
            )
        }
    }

    @Synchronized
    fun restoreStandardTabs(restored: List<BrowserTab>) {
        tabs.clear()
        restored.filter { it.mode == BrowsingMode.STANDARD }
            .distinctBy { it.id }
            .forEach { tabs += it.copy(isLoading = false, progress = 0) }
        activeId = tabs.firstOrNull()?.id
    }

    private fun update(id: String, transform: (BrowserTab) -> BrowserTab): Boolean {
        val index = tabs.indexOfFirst { it.id == id }
        if (index < 0) return false
        tabs[index] = transform(tabs[index])
        return true
    }
}
