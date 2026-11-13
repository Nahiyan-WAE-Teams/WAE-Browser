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
            val restored = withContext(Dispatchers.IO) {
                try {
                    sessionPersistence.restoreInto(sessionController)
                    true
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    Log.e(TAG, "Could not restore browser session; preserving stored data", error)
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
                            title = { Text("Saved session could not be opened") },
                            text = {
                                Text(
                                    "The saved browser session is unreadable or uses an unsupported format. " +
                                        "Its stored data has been preserved. Resetting will permanently " +
                                        "discard that saved session and start with a new tab."
                                )
                            },
                            confirmButton = {
                                Button(
                                    enabled = !recoveryBusy,
                                    onClick = {
                                        if (recoveryBusy) return@Button
                                        recoveryBusy = true
                                        lifecycleScope.launch {
                                            try {
                                                withContext(Dispatchers.IO) {
                                                    sessionRepository.clear()
                                                }
                                                sessionController.createTab()
                                                sessionPersistenceAllowed = true
                                                sessionReady = true
                                                recoveryComplete = true
                                            } catch (error: CancellationException) {
                                                throw error
                                            } catch (error: Exception) {
                                                recoveryBusy = false
                                                Log.e(TAG, "Could not reset the unreadable browser session", error)
                                            }
                                        }
                                    }
                                ) {
                                    Text(if (recoveryBusy) "Resetting…" else "Reset saved session")
                                }
                            },
                            dismissButton = {
                                Button(
                                    enabled = !recoveryBusy,
                                    onClick = { finish() }
                                ) {
                                    Text("Exit without resetting")
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
