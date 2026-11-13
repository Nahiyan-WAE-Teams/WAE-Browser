package com.wae.browser.core

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.URI

/**
 * A stored session exists but cannot safely be interpreted or restored.
 * Transient storage failures remain ordinary IOExceptions and must not
 * trigger a destructive-reset flow.
 */
class StoredSessionFormatException(
    message: String,
    cause: Throwable? = null
) : IOException(message, cause)

/**
 * Android-backed session storage.
 *
 * Only the standard-tab projection is accepted by this repository. Address-bar
 * drafts and transient engine state are intentionally not serialized.
 */
class DataStoreBrowserSessionRepository(
    private val dataStore: DataStore<Preferences>
) : BrowserSessionRepository {

    override suspend fun read(): PersistedBrowserSession {
        val encoded = dataStore.data.first()[SESSION_JSON] ?: return EMPTY_SESSION
        if (encoded.length > MAX_JSON_CHARS) {
            throw StoredSessionFormatException("Stored browser session exceeds the size limit")
        }

        return try {
            decode(encoded)
        } catch (error: JSONException) {
            throw StoredSessionFormatException("Stored browser session is malformed", error)
        } catch (error: IllegalArgumentException) {
            throw StoredSessionFormatException("Stored browser session violates its schema", error)
        }
    }

    override suspend fun write(session: PersistedBrowserSession) {
        require(session.tabs.size <= MAX_TABS) { "Too many tabs to persist" }
        val encoded = encode(session)
        if (encoded.length > MAX_JSON_CHARS) {
            throw IOException("Browser session exceeds the storage size limit")
        }
        dataStore.edit { preferences ->
            preferences[SESSION_JSON] = encoded
        }
    }

    /**
     * Explicitly discard the stored session after the user confirms recovery.
     * This is intentionally not called automatically when decoding fails.
     */
    suspend fun clear() {
        dataStore.edit { preferences ->
            preferences.remove(SESSION_JSON)
        }
    }

    private fun encode(session: PersistedBrowserSession): String {
        val root = JSONObject()
            .put("schemaVersion", SCHEMA_VERSION)
            .put("activeTabId", session.activeTabId ?: JSONObject.NULL)
        val tabs = JSONArray()

        session.tabs.forEach { tab ->
            require(tab.id.length <= MAX_ID_CHARS)
            require(tab.title.length <= MAX_TITLE_CHARS)
            require(tab.currentUrl == null || tab.currentUrl.length <= MAX_URL_CHARS)
            require(tab.currentUrl == null || isRestorableWebUrl(tab.currentUrl)) {
                "Only valid HTTP(S) URLs can be persisted"
            }
            tabs.put(
                JSONObject()
                    .put("id", tab.id)
                    .put("title", tab.title)
                    .put("currentUrl", tab.currentUrl ?: JSONObject.NULL)
                    .put("createdAtEpochMillis", tab.createdAtEpochMillis)
            )
        }
        root.put("tabs", tabs)
        return root.toString()
    }

    private fun decode(encoded: String): PersistedBrowserSession {
        val root = JSONObject(encoded)
        require(root.getInt("schemaVersion") == SCHEMA_VERSION) {
            "Unsupported browser session schema"
        }

        val tabsJson = root.getJSONArray("tabs")
        require(tabsJson.length() <= MAX_TABS) { "Stored session has too many tabs" }
        val tabs = ArrayList<BrowserTab>(tabsJson.length())

        for (index in 0 until tabsJson.length()) {
            val item = tabsJson.getJSONObject(index)
            val id = item.getString("id")
            val title = item.getString("title")
            val currentUrl = if (item.isNull("currentUrl")) null
                else item.getString("currentUrl")
            require(id.isNotBlank() && id.length <= MAX_ID_CHARS)
            require(title.length <= MAX_TITLE_CHARS)
            require(currentUrl == null || currentUrl.length <= MAX_URL_CHARS)
            require(currentUrl == null || isRestorableWebUrl(currentUrl)) {
                "Stored tab URL is not a valid HTTP(S) URL"
            }

            tabs += BrowserTab(
                id = id,
                mode = BrowsingMode.STANDARD,
                title = title,
                currentUrl = currentUrl,
                // Never restore an unsent address-bar draft.
                pendingInput = currentUrl.orEmpty(),
                isLoading = false,
                progress = 0,
                canGoBack = false,
                canGoForward = false,
                security = NavigationSecurity.UNKNOWN,
                createdAtEpochMillis = item.getLong("createdAtEpochMillis")
            )
        }

        val activeId = if (root.isNull("activeTabId")) null
            else root.getString("activeTabId")
        return PersistedBrowserSession(tabs, activeId)
    }

    private fun isRestorableWebUrl(value: String): Boolean = try {
        val uri = URI(value)
        (uri.scheme.equals("https", ignoreCase = true) ||
            uri.scheme.equals("http", ignoreCase = true)) &&
            !uri.host.isNullOrBlank() &&
            uri.rawUserInfo == null &&
            (uri.port == -1 || uri.port in 1..65535)
    } catch (_: Exception) {
        false
    }

    companion object {
        private const val FILE_NAME = "wae_browser_session.preferences_pb"
        private const val SCHEMA_VERSION = 1
        private const val MAX_TABS = 50
        private const val MAX_ID_CHARS = 128
        private const val MAX_TITLE_CHARS = 512
        private const val MAX_URL_CHARS = 8_192
        private const val MAX_JSON_CHARS = 512 * 1_024

        private val SESSION_JSON = stringPreferencesKey("standard_session_json")
        private val EMPTY_SESSION = PersistedBrowserSession(emptyList(), null)

        /**
         * Create once per application process and reuse the returned instance.
         * DataStore requires a single instance per backing file.
         */
        fun create(context: Context): DataStoreBrowserSessionRepository {
            val appContext = context.applicationContext
            val store = PreferenceDataStoreFactory.create(
                produceFile = { appContext.preferencesDataStoreFile(FILE_NAME) }
            )
            return DataStoreBrowserSessionRepository(store)
        }
    }
}
