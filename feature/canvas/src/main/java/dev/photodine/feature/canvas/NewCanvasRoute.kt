package dev.photodine.feature.canvas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import dev.photodine.core.engine.CanvasSize

/**
 * New-canvas flow: preset sizes plus custom WxH input in a bottom sheet.
 * Confirming initialises the engine and navigates to the canvas screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewCanvasRoute(
    onCanvasCreated: (width: Int, height: Int) -> Unit,
    viewModel: NewCanvasViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
    val galleryLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                val bitmap = ImageImporter.decodeBitmap(context, uri)
                if (bitmap != null) {
                    onCanvasCreated(bitmap.width, bitmap.height)
                }
            }
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is NewCanvasViewModel.NewCanvasEffect.NavigateToCanvas ->
                    onCanvasCreated(effect.width, effect.height)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = {},
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("New canvas", style = MaterialTheme.typography.headlineSmall)

            androidx.compose.material3.OutlinedButton(
                onClick = { galleryLauncher.launch("image/*") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Open from Gallery")
            }

            Text("Presets", style = MaterialTheme.typography.labelLarge)
            CanvasSize.PRESETS.forEach { preset ->
                Button(
                    onClick = {
                        viewModel.onIntent(
                            NewCanvasViewModel.NewCanvasIntent.PresetSelected(preset)
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("${preset.width} × ${preset.height}")
                }
            }

            Text("Custom size (max ${CanvasSize.MAX_DIMENSION}px)", style = MaterialTheme.typography.labelLarge)
            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = state.customWidth,
                    onValueChange = {
                        viewModel.onIntent(NewCanvasViewModel.NewCanvasIntent.WidthChanged(it))
                    },
                    label = { Text("Width") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(12.dp))
                OutlinedTextField(
                    value = state.customHeight,
                    onValueChange = {
                        viewModel.onIntent(NewCanvasViewModel.NewCanvasIntent.HeightChanged(it))
                    },
                    label = { Text("Height") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
            state.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }
            Button(
                onClick = { viewModel.onIntent(NewCanvasViewModel.NewCanvasIntent.ConfirmCustom) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Create canvas")
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
