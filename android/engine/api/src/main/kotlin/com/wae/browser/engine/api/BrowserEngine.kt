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
     * Creates an independent tab. Private tabs must not persist history,
     * cookies, cache, or other private browsing state beyond their lifetime.
     */
    fun createTab(context: Context, privateMode: Boolean = false): BrowserTab

    override fun close()
}

/** A single top-level browsing context owned by the engine. */
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
     * Requests are paired with a handler so UI/policy code, not renderer
     * content, decides permissions and file selection.
     */
    fun onEvent(event: BrowserTabEvent, requestHandler: BrowserTabRequestHandler)
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
