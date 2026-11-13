package com.wae.browser

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
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
import java.util.UUID

private data class WorkspaceTab(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "New tab",
    val address: String = ""
)

@Composable
internal fun BrowserWorkspace() {
    val tabs = remember { mutableStateListOf(WorkspaceTab()) }
    var activeTabId by remember { mutableStateOf(tabs.first().id) }
    var addressDraft by remember { mutableStateOf("") }
    var selectedSection by remember { mutableStateOf("Home") }
    val activeTab = tabs.firstOrNull { it.id == activeTabId } ?: tabs.first()

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
                        onClick = {
                            activeTabId = tab.id
                            addressDraft = tab.address
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
                                modifier = Modifier.padding(start = 8.dp),
                                fontSize = 16.sp
                            )
                        }
                    }
                }
                OutlinedButton(onClick = {
                    val tab = WorkspaceTab()
                    tabs.add(tab)
                    activeTabId = tab.id
                    addressDraft = ""
                    selectedSection = "Home"
                }) { Text("+") }
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
                        onValueChange = { addressDraft = it },
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
                    Text(
                        when (selectedSection) {
                            "Downloads" -> "Downloads"
                            "Bookmarks" -> "Bookmarks"
                            "Privacy" -> "Privacy controls"
                            else -> "A quieter place to browse"
                        },
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        when (selectedSection) {
                            "Downloads" -> "Your download queue and saved files will appear here when the browser engine is integrated."
                            "Bookmarks" -> "Saved pages and bookmark organization will be available here."
                            "Privacy" -> "Site permissions, tracking protection, cookies and network controls will be managed here."
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
                        Column(Modifier.fillMaxWidth().padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("CURRENT TAB", fontSize = 10.sp, letterSpacing = 1.5.sp,
                                color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                            Text(activeTab.title, fontWeight = FontWeight.Medium)
                            Text("Navigation engine pending", fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.secondary)
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
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }
        }
    }
}
