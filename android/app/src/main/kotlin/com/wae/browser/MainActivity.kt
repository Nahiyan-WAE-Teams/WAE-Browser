package com.wae.browser

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.wae.browser.core.BrowserSessionController
import com.wae.browser.core.DataStoreBrowserBookmarkRepository
import com.wae.browser.core.BrowserSessionPersistence
import com.wae.browser.core.DataStoreBrowserSessionRepository
import com.wae.browser.core.StoredSessionFormatException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private lateinit var sessionController: BrowserSessionController
    private lateinit var sessionPersistence: BrowserSessionPersistence
    private lateinit var sessionRepository: DataStoreBrowserSessionRepository
    private var sessionReady = false
    private var sessionPersistenceAllowed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sessionController = BrowserSessionController()
        sessionRepository = BrowserSessionRepositoryProvider.get(applicationContext)
        sessionPersistence = BrowserSessionPersistence(sessionRepository)
        val bookmarkRepository = BrowserBookmarkRepositoryProvider.get(applicationContext)

        lifecycleScope.launch {
            var resetRequired = false
            var restoreError: String? = null
            val restored = withContext(Dispatchers.IO) {
                try {
                    sessionPersistence.restoreInto(sessionController)
                    true
                } catch (error: CancellationException) {
                    throw error
                } catch (error: StoredSessionFormatException) {
                    resetRequired = true
                    restoreError = error.message
                    Log.e(TAG, "Stored browser session is malformed or unsupported", error)
                    false
                } catch (error: Exception) {
                    restoreError = "Session storage is temporarily unavailable. Retry without resetting."
                    Log.e(TAG, "Could not read browser session; preserving stored data", error)
                    false
                }
            }
            sessionPersistenceAllowed = restored
            if (restored) {
                if (sessionController.snapshot().tabs.isEmpty()) {
                    sessionController.createTab()
                }
                sessionReady = true
            }

            setContent {
                var recoveryComplete by remember { mutableStateOf(restored) }
                var recoveryRequiresReset by remember { mutableStateOf(resetRequired) }
                var recoveryError by remember { mutableStateOf(restoreError) }
                var recoveryBusy by remember { mutableStateOf(false) }
                MaterialTheme(
                    colorScheme = darkColorScheme(
                        primary = Color(0xFFB5C8FF),
                        onPrimary = Color(0xFF14264D),
                        secondary = Color(0xFFB7C8E8),
                        background = Color(0xFF090D16),
                        surface = Color(0xFF111827),
                        surfaceVariant = Color(0xFF1B2638),
                        onBackground = Color(0xFFE7ECF5),
                        onSurface = Color(0xFFE7ECF5)
                    )
                ) {
                    if (!recoveryComplete) {
                        AlertDialog(
                            onDismissRequest = {},
                            title = {
                                Text(
                                    if (recoveryRequiresReset) {
                                        "Saved session needs recovery"
                                    } else {
                                        "Session storage is unavailable"
                                    }
                                )
                            },
                            text = {
                                androidx.compose.foundation.layout.Column {
                                    Text(
                                        if (recoveryRequiresReset) {
                                            "The saved browser session is malformed or uses an unsupported format. " +
                                                "Its stored data has been preserved. Resetting permanently discards " +
                                                "that saved session and starts with a new tab."
                                        } else {
                                            "The saved session could not be read. This may be a temporary storage " +
                                                "problem; retry before considering any data reset."
                                        }
                                    )
                                    if (recoveryError != null) {
                                        Text(recoveryError!!)
                                    }
                                }
                            },
                            confirmButton = {
                                Button(
                                    enabled = !recoveryBusy,
                                    onClick = {
                                        if (recoveryBusy) return@Button
                                        recoveryBusy = true
                                        recoveryError = null
                                        lifecycleScope.launch {
                                            try {
                                                if (recoveryRequiresReset) {
                                                    withContext(Dispatchers.IO) {
                                                        BrowserSessionRepositoryProvider.clear(sessionRepository)
                                                    }
                                                    sessionController.createTab()
                                                } else {
                                                    withContext(Dispatchers.IO) {
                                                        sessionPersistence.restoreInto(sessionController)
                                                    }
                                                    if (sessionController.snapshot().tabs.isEmpty()) {
                                                        sessionController.createTab()
                                                    }
                                                }
                                                sessionPersistenceAllowed = true
                                                sessionReady = true
                                                recoveryComplete = true
                                            } catch (error: CancellationException) {
                                                throw error
                                            } catch (error: StoredSessionFormatException) {
                                                recoveryRequiresReset = true
                                                recoveryError = "The saved session is malformed or unsupported. " +
                                                    "You can reset it explicitly, or exit without changing it."
                                                Log.e(TAG, "Session retry found invalid stored data", error)
                                            } catch (error: Exception) {
                                                recoveryError = if (recoveryRequiresReset) {
                                                    "Reset failed. The saved data was not intentionally replaced. " +
                                                        "Retry the reset or exit without resetting."
                                                } else {
                                                    "Storage is still unavailable. No saved data was reset; retry later."
                                                }
                                                Log.e(TAG, "Session recovery action failed", error)
                                            } finally {
                                                recoveryBusy = false
                                            }
                                        }
                                    }
                                ) {
                                    Text(
                                        when {
                                            recoveryBusy && recoveryRequiresReset -> "Resetting…"
                                            recoveryBusy -> "Retrying…"
                                            recoveryRequiresReset -> "Reset saved session"
                                            else -> "Retry"
                                        }
                                    )
                                }
                            },
                            dismissButton = {
                                Button(
                                    enabled = !recoveryBusy,
                                    onClick = { finish() }
                                ) {
                                    Text("Exit without changing saved data")
                                }
                            }
                        )
                    } else {
                        BrowserWorkspace(
                            sessionController = sessionController,
                            bookmarkRepository = bookmarkRepository,
                            onSessionChanged = ::persistSession
                        )
                    }
                }
            }
        }
    }

    override fun onStop() {
        if (sessionReady && ::sessionController.isInitialized && ::sessionPersistence.isInitialized) {
            persistSession()
        }
        super.onStop()
    }

    private fun persistSession() {
        if (!sessionPersistenceAllowed) return
        BrowserSessionRepositoryProvider.persist(sessionPersistence, sessionController)
    }

    private companion object {
        const val TAG = "WaeBrowserSession"
    }
}

private object BrowserSessionRepositoryProvider {
    private val persistenceScope = kotlinx.coroutines.CoroutineScope(
        kotlinx.coroutines.SupervisorJob() + Dispatchers.IO
    )
    private val writeMutex = Mutex()

    fun persist(persistence: BrowserSessionPersistence, controller: BrowserSessionController) {
        persistenceScope.launch {
            writeMutex.withLock {
                try {
                    persistence.saveFrom(controller)
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    Log.e("WaeBrowserSession", "Could not persist browser session", error)
                }
            }
        }
    }

    suspend fun clear(repository: DataStoreBrowserSessionRepository) {
        writeMutex.withLock {
            repository.clear()
        }
    }

    @Volatile
    private var instance: DataStoreBrowserSessionRepository? = null

    fun get(context: android.content.Context): DataStoreBrowserSessionRepository =
        instance ?: synchronized(this) {
            instance ?: DataStoreBrowserSessionRepository.create(context).also {
                instance = it
            }
        }
}

private object BrowserBookmarkRepositoryProvider {
    @Volatile
    private var instance: DataStoreBrowserBookmarkRepository? = null

    fun get(context: android.content.Context): DataStoreBrowserBookmarkRepository =
        instance ?: synchronized(this) {
            instance ?: DataStoreBrowserBookmarkRepository.create(context).also {
                instance = it
            }
        }
}
