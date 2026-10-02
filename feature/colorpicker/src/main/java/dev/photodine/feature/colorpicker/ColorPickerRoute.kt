package dev.photodine.feature.colorpicker

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorPickerBottomSheet(
    onDismissRequest: () -> Unit,
    onColorSelected: (Int) -> Unit,
    onEyedropperClicked: () -> Unit,
    viewModel: ColorPickerViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    ModalBottomSheet(
        onDismissRequest = {
            viewModel.onIntent(ColorPickerIntent.ClosePicker)
            onDismissRequest()
        },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Color Picker", style = MaterialTheme.typography.titleLarge)

                // Current color swatch preview + eyedropper button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(state.argbColor))
                            .border(2.dp, Color.Gray, CircleShape)
                    )
                    OutlinedButton(onClick = {
                        viewModel.onIntent(ColorPickerIntent.ActivateEyedropper)
                        onEyedropperClicked()
                    }) {
                        Text("🔍 Eyedropper")
                    }
                }
            }

            // Hex input field
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Hex:", style = MaterialTheme.typography.labelLarge)
                OutlinedTextField(
                    value = state.hexString,
                    onValueChange = { viewModel.onIntent(ColorPickerIntent.SetHex(it)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            // RGB Sliders
            Text("RGB Channels", style = MaterialTheme.typography.labelLarge)
            ColorChannelSlider(
                label = "R: ${state.red}",
                value = state.red.toFloat(),
                valueRange = 0f..255f,
                accentColor = Color.Red,
                onValueChange = { viewModel.onIntent(ColorPickerIntent.SetRgb(it.toInt(), state.green, state.blue)) }
            )
            ColorChannelSlider(
                label = "G: ${state.green}",
                value = state.green.toFloat(),
                valueRange = 0f..255f,
                accentColor = Color.Green,
                onValueChange = { viewModel.onIntent(ColorPickerIntent.SetRgb(state.red, it.toInt(), state.blue)) }
            )
            ColorChannelSlider(
                label = "B: ${state.blue}",
                value = state.blue.toFloat(),
                valueRange = 0f..255f,
                accentColor = Color.Blue,
                onValueChange = { viewModel.onIntent(ColorPickerIntent.SetRgb(state.red, state.green, it.toInt())) }
            )

            // Opacity (Alpha) Slider
            ColorChannelSlider(
                label = "Opacity: ${(state.alpha * 100).toInt()}%",
                value = state.alpha,
                valueRange = 0f..1f,
                accentColor = Color.Gray,
                onValueChange = { viewModel.onIntent(ColorPickerIntent.SetAlpha(it)) }
            )

            // Recent colors
            if (state.recentColors.isNotEmpty()) {
                Text("Recent Colors", style = MaterialTheme.typography.labelLarge)
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.recentColors) { colorInt ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(colorInt))
                                .border(1.dp, Color.White, CircleShape)
                                .clickable {
                                    viewModel.onIntent(ColorPickerIntent.SelectRecent(colorInt))
                                    onColorSelected(colorInt)
                                    onDismissRequest()
                                }
                        )
                    }
                }
            }

            Button(
                onClick = {
                    viewModel.onIntent(ColorPickerIntent.ClosePicker)
                    onColorSelected(state.argbColor)
                    onDismissRequest()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Select Color")
            }
        }
    }
}

@Composable
private fun ColorChannelSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    accentColor: Color,
    onValueChange: (Float) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.width(90.dp)
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = accentColor,
                activeTrackColor = accentColor
            ),
            modifier = Modifier.weight(1f)
        )
    }
}
