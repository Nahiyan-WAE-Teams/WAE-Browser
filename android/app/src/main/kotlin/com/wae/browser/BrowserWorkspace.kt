package com.wae.browser

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wae.browser.core.AddressBarResolver
import com.wae.browser.core.BrowserBookmark
import com.wae.browser.core.BrowserSessionController
import com.wae.browser.core.DataStoreBrowserBookmarkRepository
import com.wae.browser.core.NavigationTarget
import kotlinx.coroutines.launch
import com.wae.browser.core.BrowsingMode

@Composable
internal fun BrowserWorkspace(
    sessionController: BrowserSessionController,
    bookmarkRepository: DataStoreBrowserBookmarkRepository,
    onSessionChanged: () -> Unit
) {
    var session by remember { mutableStateOf(sessionController.snapshot()) }
    val tabs = session.tabs
    val activeTab = session.activeTab ?: tabs.first()
    val activeTabId = activeTab.id
    var addressDraft by remember { mutableStateOf(activeTab.pendingInput) }
    var selectedSection by remember { mutableStateOf("Home") }
    var bookmarks by remember { mutableStateOf(emptyList<BrowserBookmark>()) }
    var bookmarkMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(bookmarkRepository) {
        runCatching { bookmarkRepository.readAll() }
            .onSuccess { bookmarks = it }
            .onFailure { bookmarkMessage = "Could not load bookmarks" }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("WAE", color = MaterialTheme.colorScheme.primary,
                        fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
                    Text("Browser", fontSize = 27.sp, fontWeight = FontWeight.SemiBold)
                }
                Surface(shape = CircleShape, color = Color(0xFF263A32)) {
                    Text("ENGINE OFFLINE", color = Color(0xFFB5E5C8),
                        fontSize = 9.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp))
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                tabs.forEach { tab ->
                    val selected = tab.id == activeTabId
                    Surface(
                        modifier = Modifier.clickable {
                            sessionController.selectTab(tab.id)
                            onSessionChanged()
                            session = sessionController.snapshot()
                            addressDraft = tab.pendingInput
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = if (selected) MaterialTheme.colorScheme.surfaceVariant
                            else MaterialTheme.colorScheme.surface
                    ) {
                        Row(
                            modifier = Modifier.padding(start = 12.dp, end = 8.dp, top = 9.dp, bottom = 9.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(tab.title, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                fontSize = 12.sp, modifier = Modifier.width(84.dp))
                            Text(
                                "×",
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(start = 8.dp).clickable {
                                    sessionController.closeTab(tab.id)
                                    if (sessionController.snapshot().tabs.isEmpty()) {
                                        sessionController.createTab()
                                    }
                                    onSessionChanged()
                                    session = sessionController.snapshot()
                                    addressDraft = session.activeTab?.pendingInput.orEmpty()
                                },
                                fontSize = 16.sp
                            )
                        }
                    }
                }
                OutlinedButton(onClick = {
                    val tab = sessionController.createTab()
                    onSessionChanged()
                    session = sessionController.snapshot()
                    addressDraft = tab.pendingInput
                    selectedSection = "Home"
                }) { Text("+") }
                OutlinedButton(onClick = {
                    val tab = sessionController.createTab(BrowsingMode.PRIVATE)
                    onSessionChanged()
                    session = sessionController.snapshot()
                    addressDraft = tab.pendingInput
                    selectedSection = "Home"
                }) { Text("Private") }
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("⌕", fontSize = 20.sp, color = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.width(10.dp))
                    BasicTextField(
                        value = addressDraft,
                        onValueChange = {
                            val boundedInput = it.take(8_192)
                            addressDraft = boundedInput
                            sessionController.updatePendingInput(activeTab.id, boundedInput)
                            session = sessionController.snapshot()
                        },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        decorationBox = { inner ->
                            if (addressDraft.isEmpty()) {
                                Text("Search or enter address", color = MaterialTheme.colorScheme.secondary)
                            }
                            inner()
                        }
                    )
                    Text("⋮", fontSize = 22.sp, color = MaterialTheme.colorScheme.secondary)
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth().weight(1f),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (selectedSection == "Bookmarks") {
                        Text("Bookmarks", fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            "Saved locally on this device. Select an item to place its address in the address bar.",
                            color = MaterialTheme.colorScheme.secondary,
                            fontSize = 13.sp,
                            lineHeight = 19.sp
                        )
                        Button(
                            onClick = {
                                val candidate = activeTab.currentUrl ?: addressDraft
                                val uri = when (val target = AddressBarResolver().resolve(candidate)) {
                                    is NavigationTarget.WebUrl -> target.uri
                                    is NavigationTarget.SearchQuery -> target.uri
                                    is NavigationTarget.Rejected -> null
                                }
                                if (uri == null) {
                                    bookmarkMessage = "Enter a valid web address or search first"
                                } else {
                                    coroutineScope.launch {
                                        runCatching {
                                            bookmarkRepository.add(
                                                title = uri.host ?: uri.toString(),
                                                rawUrl = uri.toString()
                                            )
                                            bookmarkRepository.readAll()
                                        }.onSuccess {
                                            bookmarks = it
                                            bookmarkMessage = "Bookmark saved"
                                        }.onFailure {
                                            bookmarkMessage = it.message ?: "Could not save bookmark"
                                        }
                                    }
                                }
                            },
                            enabled = activeTab.currentUrl != null || addressDraft.isNotBlank()
                        ) {
                            Text("Save address")
                        }
                        bookmarkMessage?.let {
                            Text(it, color = MaterialTheme.colorScheme.secondary, fontSize = 12.sp)
                        }
                        if (bookmarks.isEmpty()) {
                            Text(
                                "No bookmarks yet",
                                color = MaterialTheme.colorScheme.secondary,
                                fontSize = 14.sp
                            )
                        } else {
                            Column(
                                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                bookmarks.forEach { bookmark ->
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(
                                                modifier = Modifier.weight(1f).clickable {
                                                    addressDraft = bookmark.url
                                                    sessionController.updatePendingInput(activeTab.id, bookmark.url)
                                                    session = sessionController.snapshot()
                                                    bookmarkMessage = "Address copied to the address bar"
                                                }
                                            ) {
                                                Text(
                                                    bookmark.title,
                                                    fontWeight = FontWeight.Medium,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    bookmark.url,
                                                    color = MaterialTheme.colorScheme.secondary,
                                                    fontSize = 11.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            Text(
                                                "Remove",
                                                color = MaterialTheme.colorScheme.secondary,
                                                modifier = Modifier.padding(start = 12.dp).clickable {
                                                    coroutineScope.launch {
                                                        runCatching {
                                                            bookmarkRepository.remove(bookmark.id)
                                                            bookmarkRepository.readAll()
                                                        }.onSuccess {
                                                            bookmarks = it
                                                            bookmarkMessage = "Bookmark removed"
                                                        }.onFailure {
                                                            bookmarkMessage = "Could not remove bookmark"
                                                        }
                                                    }
                                                },
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Text(
                            when (selectedSection) {
                                "Downloads" -> "Downloads"
                                "Privacy" -> "Privacy controls"
                                else -> "A quieter place to browse"
                            },
                            fontSize = 22.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            when (selectedSection) {
                                "Downloads" -> "The download queue will connect to Chromium's download backend during runtime integration."
                                "Privacy" -> "Site permissions, tracking protection, cookies and network controls will be integrated with Chromium's browser-owned security settings."
                                else -> "WAE Browser is being built around a dedicated Chromium integration. Your tabs and workspace are local; page navigation is not enabled yet."
                            },
                            color = MaterialTheme.colorScheme.secondary,
                            fontSize = 14.sp,
                            lineHeight = 21.sp
                        )
                        Spacer(Modifier.weight(1f))
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                Modifier.fillMaxWidth().padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    "CURRENT TAB",
                                    fontSize = 10.sp,
                                    letterSpacing = 1.5.sp,
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(activeTab.title, fontWeight = FontWeight.Medium)
                                Text(
                                    if (activeTab.mode == BrowsingMode.PRIVATE) {
                                        "PRIVATE TAB · PROFILE ISOLATION PENDING"
                                    } else {
                                        "STANDARD TAB · ENGINE PENDING"
                                    },
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Navigation engine pending",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf("Home", "Bookmarks", "Downloads", "Privacy").forEach { item ->
                    val active = selectedSection == item
                    Text(
                        item,
                        color = if (active) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.secondary,
                        fontSize = 11.sp,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.padding(vertical = 8.dp).clickable { selectedSection = item }
                    )
                }
            }
        }
    }
}
