package dev.photodine.feature.export

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportBottomSheet(
    onDismissRequest: () -> Unit,
    snackbarHostState: SnackbarHostState? = null,
    viewModel: ExportViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is ExportEffect.ShareImage -> {
                    val shareIntent = ExportManager.createShareIntent(effect.uri, effect.mimeType)
                    context.startActivity(android.content.Intent.createChooser(shareIntent, "Share image"))
                }
                is ExportEffect.ShowSnackbar -> {
                    snackbarHostState?.showSnackbar(effect.message)
                }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Export Image",
                style = MaterialTheme.typography.titleLarge
            )

            // Format Selector: PNG / JPEG
            Text(text = "Format", style = MaterialTheme.typography.labelLarge)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ExportFormat.entries.forEach { format ->
                    FilterChip(
                        selected = state.format == format,
                        onClick = { viewModel.onIntent(ExportIntent.SetFormat(format)) },
                        label = { Text(format.name) }
                    )
                }
            }

            // Quality slider for JPEG
            if (state.format == ExportFormat.JPEG) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "JPEG Quality", style = MaterialTheme.typography.labelLarge)
                        Text(text = "${state.jpegQuality}%", style = MaterialTheme.typography.bodyMedium)
                    }
                    Slider(
                        value = state.jpegQuality.toFloat(),
                        onValueChange = { viewModel.onIntent(ExportIntent.SetQuality(it.toInt())) },
                        valueRange = 10f..100f,
                        steps = 18
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action buttons: Save to Gallery & Share
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { viewModel.onIntent(ExportIntent.Export(andShare = false)) },
                    enabled = !state.isExporting,
                    modifier = Modifier.weight(1f)
                ) {
                    if (state.isExporting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text("Save to Gallery")
                }

                OutlinedButton(
                    onClick = { viewModel.onIntent(ExportIntent.Export(andShare = true)) },
                    enabled = !state.isExporting,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Share")
                }
            }
        }
    }
}
