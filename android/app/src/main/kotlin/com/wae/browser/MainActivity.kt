package com.wae.browser

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                BrowserWorkspace()
            }
        }
    }
}
