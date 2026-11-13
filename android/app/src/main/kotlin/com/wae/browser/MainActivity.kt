package com.wae.browser

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import com.wae.browser.core.BrowserSessionController
import com.wae.browser.core.BrowserSessionPersistence
import com.wae.browser.core.DataStoreBrowserSessionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

class MainActivity : ComponentActivity() {
    private lateinit var sessionController: BrowserSessionController
    private lateinit var sessionPersistence: BrowserSessionPersistence
    private var sessionReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sessionController = BrowserSessionController()
        sessionPersistence = BrowserSessionPersistence(
            BrowserSessionRepositoryProvider.get(applicationContext)
        )

        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                runCatching { sessionPersistence.restoreInto(sessionController) }
                    .onFailure { Log.e(TAG, "Could not restore browser session", it) }
            }
            if (sessionController.snapshot().tabs.isEmpty()) {
                sessionController.createTab()
            }
            sessionReady = true

            setContent {
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
                    BrowserWorkspace(
                        sessionController = sessionController,
                        onSessionChanged = ::persistSession
                    )
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
        BrowserSessionRepositoryProvider.persist(sessionPersistence, sessionController)
    }

    private companion object {
        const val TAG = "WaeBrowserSession"
    }
}

private object BrowserSessionRepositoryProvider {
    private val persistenceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val writeMutex = Mutex()

    fun persist(persistence: BrowserSessionPersistence, controller: BrowserSessionController) {
        persistenceScope.launch {
            writeMutex.withLock {
                runCatching { persistence.saveFrom(controller) }
                    .onFailure { Log.e("WaeBrowserSession", "Could not persist browser session", it) }
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
