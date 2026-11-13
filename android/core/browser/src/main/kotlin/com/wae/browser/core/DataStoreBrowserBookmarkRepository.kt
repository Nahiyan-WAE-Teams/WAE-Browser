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
import org.json.JSONObject
import java.io.IOException
import java.net.URI
import java.util.UUID

data class BrowserBookmark(
    val id: String,
    val title: String,
    val url: String,
    val createdAtEpochMillis: Long
)

/**
 * Local-only bookmark storage. It is deliberately independent of browser
 * navigation so it can be retained when the Chromium runtime is integrated.
 */
class DataStoreBrowserBookmarkRepository(
    private val dataStore: DataStore<Preferences>,
    private val now: () -> Long = System::currentTimeMillis,
    private val newId: () -> String = { UUID.randomUUID().toString() }
) {
    suspend fun readAll(): List<BrowserBookmark> {
        val encoded = dataStore.data.first()[BOOKMARKS_JSON] ?: return emptyList()
        if (encoded.length > MAX_JSON_CHARS) {
            throw IOException("Stored bookmarks exceed the size limit")
        }
        return try {
            decode(encoded)
        } catch (error: Exception) {
            throw IOException("Stored bookmarks are malformed or violate their schema", error)
        }
    }

    suspend fun add(title: String, rawUrl: String): BrowserBookmark {
        val url = normalizeWebUrl(rawUrl)
        val safeTitle = title.trim().ifBlank { URI(url).host.orEmpty() }.take(MAX_TITLE_CHARS)
        val bookmark = BrowserBookmark(
            id = newId(),
            title = safeTitle,
            url = url,
            createdAtEpochMillis = now()
        )
        require(bookmark.id.isNotBlank() && bookmark.id.length <= MAX_ID_CHARS)

        var savedBookmark = bookmark
        dataStore.edit { preferences ->
            val current = decode(preferences[BOOKMARKS_JSON].orEmpty())
            val existingIndex = current.indexOfFirst { it.url == url }
            val updated = if (existingIndex >= 0) {
                current.toMutableList().also {
                    savedBookmark = current[existingIndex].copy(title = safeTitle)
                    it[existingIndex] = savedBookmark
                }
            } else {
                require(current.size < MAX_BOOKMARKS) { "Bookmark limit reached" }
                current + bookmark
            }
            val encoded = encode(updated)
            if (encoded.length > MAX_JSON_CHARS) {
                throw IOException("Bookmark collection exceeds the storage size limit")
            }
            preferences[BOOKMARKS_JSON] = encoded
        }
        return savedBookmark
    }

    suspend fun remove(id: String) {
        require(id.isNotBlank() && id.length <= MAX_ID_CHARS)
        dataStore.edit { preferences ->
            val remaining = decode(preferences[BOOKMARKS_JSON].orEmpty()).filterNot { it.id == id }
            preferences[BOOKMARKS_JSON] = encode(remaining)
        }
    }

    private fun encode(bookmarks: List<BrowserBookmark>): String {
        require(bookmarks.size <= MAX_BOOKMARKS)
        val array = JSONArray()
        bookmarks.forEach { bookmark ->
            require(bookmark.id.isNotBlank() && bookmark.id.length <= MAX_ID_CHARS)
            require(bookmark.title.length <= MAX_TITLE_CHARS)
            require(bookmark.url.length <= MAX_URL_CHARS)
            require(normalizeWebUrl(bookmark.url) == bookmark.url)
            array.put(
                JSONObject()
                    .put("id", bookmark.id)
                    .put("title", bookmark.title)
                    .put("url", bookmark.url)
                    .put("createdAtEpochMillis", bookmark.createdAtEpochMillis)
            )
        }
        return array.toString()
    }

    private fun decode(encoded: String): List<BrowserBookmark> {
        if (encoded.isBlank()) return emptyList()
        val array = JSONArray(encoded)
        require(array.length() <= MAX_BOOKMARKS)
        val result = ArrayList<BrowserBookmark>(array.length())
        for (index in 0 until array.length()) {
            val item = array.getJSONObject(index)
            val bookmark = BrowserBookmark(
                id = item.getString("id"),
                title = item.getString("title"),
                url = item.getString("url"),
                createdAtEpochMillis = item.getLong("createdAtEpochMillis")
            )
            require(bookmark.id.isNotBlank() && bookmark.id.length <= MAX_ID_CHARS)
            require(bookmark.title.length <= MAX_TITLE_CHARS)
            require(normalizeWebUrl(bookmark.url) == bookmark.url)
            result += bookmark
        }
        require(result.map { it.id }.distinct().size == result.size)
        require(result.map { it.url }.distinct().size == result.size)
        return result
    }

    private fun normalizeWebUrl(rawUrl: String): String {
        require(rawUrl.length <= MAX_URL_CHARS) { "URL exceeds the size limit" }
        val uri = URI(rawUrl.trim()).normalize()
        require(uri.scheme.equals("https", true) || uri.scheme.equals("http", true)) {
            "Only HTTP and HTTPS bookmarks are supported"
        }
        require(!uri.host.isNullOrBlank() && uri.rawUserInfo == null) {
            "Bookmark URL must have a host and cannot contain embedded credentials"
        }
        require((uri.port == -1 || uri.port in 1..65535)) { "Bookmark URL has an invalid port" }
        return uri.toASCIIString()
    }

    companion object {
        private const val FILE_NAME = "wae_browser_bookmarks.preferences_pb"
        private const val MAX_BOOKMARKS = 500
        private const val MAX_ID_CHARS = 128
        private const val MAX_TITLE_CHARS = 512
        private const val MAX_URL_CHARS = 8_192
        private const val MAX_JSON_CHARS = 2 * 1_024 * 1_024
        private val BOOKMARKS_JSON = stringPreferencesKey("bookmarks_json")

        fun create(context: Context): DataStoreBrowserBookmarkRepository {
            val appContext = context.applicationContext
            val store = PreferenceDataStoreFactory.create(
                produceFile = { appContext.preferencesDataStoreFile(FILE_NAME) }
            )
            return DataStoreBrowserBookmarkRepository(store)
        }
    }
}
