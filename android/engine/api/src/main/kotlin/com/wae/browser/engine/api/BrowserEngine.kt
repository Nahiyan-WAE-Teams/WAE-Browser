package com.wae.browser.engine.api

import android.content.Context
import android.net.Uri
import android.view.View

/**
 * Narrow application-facing contract for a real browser engine.
 *
 * Implementations must preserve Chromium's sandbox, site isolation,
 * certificate validation, and renderer process boundaries. This contract
 * deliberately contains no WebView implementation.
 */
interface BrowserEngine : AutoCloseable {
    val info: BrowserEngineInfo

    /**
     * Creates an independent browser profile. A private profile is an
     * off-the-record session: it must not write browsing data to durable
     * storage and must destroy its in-memory state when closed.
     */
    fun createProfile(context: Context, mode: BrowserProfileMode): BrowserProfile

    override fun close()
}

enum class BrowserProfileMode { STANDARD, PRIVATE }

/**
 * A profile owns cookies, cache, storage partitions, permissions context,
 * and tabs. Multiple private tabs may share one private profile; closing
 * that profile closes all its tabs and discards its ephemeral data.
 */
interface BrowserProfile : AutoCloseable {
    val mode: BrowserProfileMode

    fun createTab(): BrowserTab

    override fun close()
}

/** A single top-level browsing context owned by a profile. */
interface BrowserTab : AutoCloseable {
    val id: String
    val view: View
    val state: BrowserTabState

    fun navigate(uri: Uri)
    fun stopLoading()
    fun reload()
    fun goBack()
    fun goForward()

    /** Register a listener and return a handle that unregisters it on close. */
    fun observe(observer: BrowserTabObserver): AutoCloseable

    override fun close()
}

interface BrowserTabObserver {
    fun onStateChanged(state: BrowserTabState)

    /**
     * Synchronous main-frame gate. The engine must apply the returned
     * decision before committing the navigation, including redirects.
     * Renderer-originated requests must never be allowed to grant privileges.
     */
    fun onNavigationRequested(request: BrowserNavigationRequest): BrowserNavigationDecision

    /**
     * Requests are paired with a handler so UI/policy code, not renderer
     * content, decides permissions and file selection.
     */
    fun onEvent(event: BrowserTabEvent, requestHandler: BrowserTabRequestHandler)
}

data class BrowserNavigationRequest(
    val uri: Uri,
    val isMainFrame: Boolean,
    val source: BrowserNavigationSource,
    val hasUserGesture: Boolean
)

enum class BrowserNavigationSource {
    ADDRESS_BAR,
    LINK,
    REDIRECT,
    SCRIPT,
    RESTORE
}

sealed interface BrowserNavigationDecision {
    data class Allow(val uri: Uri) : BrowserNavigationDecision
    data class OpenExternal(val uri: Uri) : BrowserNavigationDecision
    data class Block(val reason: String) : BrowserNavigationDecision
}

sealed interface BrowserTabEvent {
    data class DownloadRequested(
        val requestId: String,
        val source: Uri,
        val suggestedFileName: String?,
        val mimeType: String?,
        val contentLengthBytes: Long?
    ) : BrowserTabEvent

    data class SitePermissionRequested(
        val requestId: String,
        val origin: String,
        val permission: SitePermission
    ) : BrowserTabEvent

    data class FileChooserRequested(
        val requestId: String,
        val acceptedMimeTypes: List<String>,
        val allowMultiple: Boolean
    ) : BrowserTabEvent

    data object RendererCrashed : BrowserTabEvent
}

enum class SitePermission {
    CAMERA,
    MICROPHONE,
    LOCATION,
    NOTIFICATIONS,
    CLIPBOARD_READ,
    MIDI,
    PROTECTED_MEDIA
}

/**
 * Explicit responses keep privileged decisions outside the rendering engine.
 * Unknown or expired request IDs must be rejected by the implementation.
 */
interface BrowserTabRequestHandler {
    fun decidePermission(requestId: String, granted: Boolean)
    fun completeFileChooser(requestId: String, selectedFiles: List<Uri>?)
}
