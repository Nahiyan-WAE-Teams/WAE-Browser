package com.wae.browser.engine.api

/**
 * Immutable browser state emitted by an engine tab.
 *
 * A certificate error is represented as a terminal security state for that
 * navigation. The application must never turn it into a user-overridable
 * "continue anyway" decision.
 */
data class BrowserTabState(
    val url: String?,
    val title: String?,
    val progress: Int,
    val isLoading: Boolean,
    val canGoBack: Boolean,
    val canGoForward: Boolean,
    val security: PageSecurity
) {
    init {
        require(progress in 0..100) { "progress must be between 0 and 100" }
    }
}

enum class PageSecurity {
    UNKNOWN,
    SECURE,
    INSECURE,
    CERTIFICATE_ERROR
}

/** Version and source provenance reported by the concrete engine adapter. */
data class BrowserEngineInfo(
    val productVersion: String,
    val chromiumRevision: String,
    val securityPatchLevel: String
)
