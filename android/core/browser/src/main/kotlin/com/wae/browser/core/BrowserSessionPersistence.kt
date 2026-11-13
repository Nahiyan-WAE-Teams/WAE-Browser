package com.wae.browser.core

/**
 * Durable representation of ordinary browsing tabs.
 *
 * Private tabs are structurally excluded from this type, rather than relying
 * on each storage implementation to remember to filter them.
 */
data class PersistedBrowserSession(
    val tabs: List<BrowserTab>,
    val activeTabId: String?
) {
    init {
        require(tabs.all { it.mode == BrowsingMode.STANDARD }) {
            "Private tabs must never enter durable session state"
        }
        require(tabs.map(BrowserTab::id).distinct().size == tabs.size) {
            "Persisted tab IDs must be unique"
        }
        require(activeTabId == null || tabs.any { it.id == activeTabId }) {
            "Persisted active tab must exist in the session"
        }
    }

    companion object {
        fun from(session: BrowserSession): PersistedBrowserSession {
            val standardTabs = session.persistableTabs().map { tab ->
                tab.copy(pendingInput = tab.currentUrl.orEmpty())
            }
            val activeId = session.activeTabId
                ?.takeIf { id -> standardTabs.any { it.id == id } }
            return PersistedBrowserSession(standardTabs, activeId)
        }
    }
}

/**
 * Storage boundary for standard browser sessions.
 *
 * Implementations must use app-private storage, apply atomic replacement,
 * bound the amount of restored data, and never persist private profile data.
 * Calls should be dispatched away from the Android main thread by the owner.
 */
interface BrowserSessionRepository {
    suspend fun read(): PersistedBrowserSession
    suspend fun write(session: PersistedBrowserSession)
}

/**
 * Coordinates the domain model with a durable repository. This class does
 * not choose a storage technology; the Android data layer supplies it.
 */
class BrowserSessionPersistence(
    private val repository: BrowserSessionRepository
) {
    suspend fun restoreInto(controller: BrowserSessionController) {
        val restored = repository.read()
        controller.restoreStandardTabs(restored.tabs, restored.activeTabId)
    }

    suspend fun saveFrom(controller: BrowserSessionController) {
        repository.write(PersistedBrowserSession.from(controller.snapshot()))
    }
}
