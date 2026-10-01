package dev.photodine.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

@Composable
fun PhotodineTheme(content: @Composable () -> Unit) {
    MaterialTheme(content = content)
}
