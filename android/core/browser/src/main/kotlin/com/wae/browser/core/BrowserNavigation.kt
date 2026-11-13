package com.wae.browser.core

import java.net.URI

enum class NavigationSource {
    ADDRESS_BAR,
    LINK,
    REDIRECT,
    SCRIPT,
    RESTORE
}

data class NavigationRequest(
    val uri: URI,
    val source: NavigationSource,
    val isMainFrame: Boolean,
    val hasUserGesture: Boolean = false
)

sealed interface NavigationDecision {
    data class Load(val uri: URI) : NavigationDecision
    data class OpenExternal(val uri: URI) : NavigationDecision
    data class Block(val reason: String) : NavigationDecision
}

/**
 * Central policy for destinations requested by pages or the address bar.
 *
 * The engine must call this for top-level navigations, including redirects.
 * It is not a replacement for Chromium's URL, origin, certificate, or
 * renderer security checks.
 */
class BrowserNavigationPolicy {
    fun evaluate(request: NavigationRequest): NavigationDecision {
        val uri = request.uri
        val scheme = uri.scheme?.lowercase()
            ?: return NavigationDecision.Block("Destination has no scheme")

        if (!request.isMainFrame) {
            return NavigationDecision.Load(uri)
        }

        if (uri.rawUserInfo != null) {
            return NavigationDecision.Block("Embedded credentials are not allowed")
        }

        return when (scheme) {
            "https", "http" -> {
                if (uri.host.isNullOrBlank() || (uri.port != -1 && uri.port !in 1..65535)) {
                    NavigationDecision.Block("Destination host or port is invalid")
                } else {
                    NavigationDecision.Load(uri.normalize())
                }
            }
            "mailto", "tel" -> {
                if (request.hasUserGesture &&
                    request.source in setOf(NavigationSource.LINK, NavigationSource.ADDRESS_BAR)
                ) {
                    NavigationDecision.OpenExternal(uri)
                } else {
                    NavigationDecision.Block("External actions require a direct user gesture")
                }
            }
            else -> NavigationDecision.Block("Navigation scheme is not supported")
        }
    }
}

sealed interface AddressSubmission {
    data class Navigate(val tabId: String, val uri: URI) : AddressSubmission
    data class Rejected(val message: String) : AddressSubmission
}

/**
 * Joins address resolution to session state without performing network I/O.
 * The UI can observe the resulting session and pass the URI to the engine.
 */
class BrowserNavigationCoordinator(
    private val sessions: BrowserSessionController,
    private val resolver: AddressBarResolver = AddressBarResolver(),
    private val policy: BrowserNavigationPolicy = BrowserNavigationPolicy()
) {
    @Synchronized
    fun submitAddress(tabId: String, input: String): AddressSubmission {
        val session = sessions.snapshot()
        if (session.tabs.none { it.id == tabId }) {
            return AddressSubmission.Rejected("The selected tab no longer exists")
        }

        val target = resolver.resolve(input)
        val uri = when (target) {
            is NavigationTarget.WebUrl -> target.uri
            is NavigationTarget.SearchQuery -> target.uri
            is NavigationTarget.Rejected -> return AddressSubmission.Rejected(target.reason)
        }

        return when (
            val decision = policy.evaluate(
                NavigationRequest(
                    uri = uri,
                    source = NavigationSource.ADDRESS_BAR,
                    isMainFrame = true,
                    hasUserGesture = true
                )
            )
        ) {
            is NavigationDecision.Load -> {
                if (!sessions.commitNavigation(tabId, decision.uri.toASCIIString())) {
                    AddressSubmission.Rejected("The selected tab was closed before navigation could start")
                } else {
                    AddressSubmission.Navigate(tabId, decision.uri)
                }
            }
            is NavigationDecision.OpenExternal ->
                AddressSubmission.Rejected("External destinations must be handed to the platform")
            is NavigationDecision.Block -> AddressSubmission.Rejected(decision.reason)
        }
    }
}
