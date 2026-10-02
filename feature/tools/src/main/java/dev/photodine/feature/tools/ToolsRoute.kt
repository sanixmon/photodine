package dev.photodine.feature.tools

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun ToolsRoute(
    modifier: Modifier = Modifier,
    onColorSwatchClicked: (() -> Unit)? = null,
    viewModel: ToolsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    ToolsPanel(
        state = state,
        onIntent = viewModel::onIntent,
        onColorSwatchClicked = onColorSwatchClicked,
        modifier = modifier
    )
}

@Composable
fun ToolsPanel(
    state: ToolsState,
    onIntent: (ToolsIntent) -> Unit,
    onColorSwatchClicked: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        tonalElevation = 6.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Main tool selection strip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ActiveTool.entries.forEach { tool ->
                    val isSelected = state.activeTool == tool
                    FilledTonalButton(
                        onClick = {
                            onIntent(ToolsIntent.SelectTool(tool))
                            if (!state.isOptionsVisible) {
                                onIntent(ToolsIntent.ToggleOptions)
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = if (isSelected) {
                            androidx.compose.material3.ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            androidx.compose.material3.ButtonDefaults.filledTonalButtonColors()
                        }
                    ) {
                        Text(tool.name)
                    }
                }
            }

            // Options drawer
            AnimatedVisibility(visible = state.isOptionsVisible) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        when (state.activeTool) {
                            ActiveTool.BRUSH -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Size: ${state.brushSize.toInt()}px",
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.width(80.dp)
                                    )
                                    Slider(
                                        value = state.brushSize,
                                        onValueChange = { onIntent(ToolsIntent.SetBrushSize(it)) },
                                        valueRange = 1f..500f,
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Opacity: ${(state.brushOpacity * 100).toInt()}%",
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.width(80.dp)
                                    )
                                    Slider(
                                        value = state.brushOpacity,
                                        onValueChange = { onIntent(ToolsIntent.SetBrushOpacity(it)) },
                                        valueRange = 0f..1f,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    // Color swatch
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(Color(state.brushColor))
                                            .border(2.dp, Color.White, CircleShape)
                                            .clickable { onColorSwatchClicked?.invoke() }
                                    )
                                }
                            }

                            ActiveTool.ERASER -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Size: ${state.eraserSize.toInt()}px",
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.width(80.dp)
                                    )
                                    Slider(
                                        value = state.eraserSize,
                                        onValueChange = { onIntent(ToolsIntent.SetEraserSize(it)) },
                                        valueRange = 1f..500f,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            ActiveTool.MOVE -> {
                                Text(
                                    text = "Move mode: 1-finger drag to translate, 2-finger pinch to scale, 2-finger twist to rotate.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }

                            ActiveTool.CROP -> {
                                Text(
                                    text = "Crop mode: drag handles to define region, then confirm or cancel.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
