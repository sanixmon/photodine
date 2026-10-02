package dev.photodine.feature.layers

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import dev.photodine.core.engine.BlendMode
import dev.photodine.core.engine.Layer

/**
 * Persistent, collapsible layer panel bottom sheet anchored at the bottom
 * of the canvas screen.
 */
@Composable
fun LayersRoute(
    modifier: Modifier = Modifier,
    viewModel: LayersViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    LayersPanel(
        state = state,
        onIntent = viewModel::onIntent,
        modifier = modifier
    )
}

@Composable
fun LayersPanel(
    state: LayerState,
    onIntent: (LayerIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        tonalElevation = 8.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Header bar with expand toggle and Add Layer button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { onIntent(LayerIntent.ToggleExpanded) }) {
                    Text(
                        text = "⧉ Layers (${state.layers.size})",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilledTonalButton(
                        onClick = { onIntent(LayerIntent.AddLayer()) },
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            horizontal = 8.dp,
                            vertical = 4.dp
                        )
                    ) {
                        Text("+ Layer", style = MaterialTheme.typography.labelSmall)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    TextButton(
                        onClick = { onIntent(LayerIntent.ToggleExpanded) },
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(4.dp)
                    ) {
                        Text("✕", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }

            AnimatedVisibility(visible = state.isExpanded) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    // Layers rendered top-to-bottom in the UI (highest z-order at top)
                    val reversedIndices = state.layers.indices.reversed().toList()
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(reversedIndices.size) { displayIdx ->
                            val actualIdx = reversedIndices[displayIdx]
                            val layer = state.layers[actualIdx]
                            val isActive = layer.id == state.activeLayerId

                            LayerRow(
                                layer = layer,
                                isActive = isActive,
                                canMoveUp = actualIdx < state.layers.size - 1,
                                canMoveDown = actualIdx > 0,
                                canDelete = state.layers.size > 1,
                                onSelect = { onIntent(LayerIntent.SelectLayer(layer.id)) },
                                onToggleVisibility = {
                                    onIntent(LayerIntent.SetVisibility(layer.id, !layer.visible))
                                },
                                onOpacityChange = { op ->
                                    onIntent(LayerIntent.SetOpacity(layer.id, op))
                                },
                                onBlendModeChange = { mode ->
                                    onIntent(LayerIntent.SetBlendMode(layer.id, mode))
                                },
                                onDuplicate = { onIntent(LayerIntent.DuplicateLayer(layer.id)) },
                                onDelete = { onIntent(LayerIntent.DeleteLayer(layer.id)) },
                                onMoveUp = { onIntent(LayerIntent.ReorderLayer(actualIdx, actualIdx + 1)) },
                                onMoveDown = { onIntent(LayerIntent.ReorderLayer(actualIdx, actualIdx - 1)) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LayerRow(
    layer: Layer,
    isActive: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    canDelete: Boolean,
    onSelect: () -> Unit,
    onToggleVisibility: () -> Unit,
    onOpacityChange: (Float) -> Unit,
    onBlendModeChange: (BlendMode) -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    var showControls by remember { mutableStateOf(false) }
    var showBlendMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Visibility toggle icon
                    TextButton(onClick = onToggleVisibility) {
                        Text(if (layer.visible) "👁" else "👁‍🗨")
                    }

                    // Thumbnail placeholder
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.DarkGray)
                            .border(1.dp, Color.Gray, RoundedCornerShape(4.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${(layer.opacity * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                    }

                    Column {
                        Text(
                            text = layer.name,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = layer.blendMode.name.lowercase().replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (canMoveUp) {
                        TextButton(
                            onClick = onMoveUp,
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(4.dp)
                        ) { Text("▲") }
                    }
                    if (canMoveDown) {
                        TextButton(
                            onClick = onMoveDown,
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(4.dp)
                        ) { Text("▼") }
                    }
                    TextButton(
                        onClick = { showControls = !showControls },
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            horizontal = 8.dp,
                            vertical = 4.dp
                        )
                    ) {
                        Text(if (showControls) "Hide" else "Edit")
                    }
                }
            }

            AnimatedVisibility(visible = showControls) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    // Opacity slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Opacity: ${(layer.opacity * 100).toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.width(90.dp)
                        )
                        Slider(
                            value = layer.opacity,
                            onValueChange = onOpacityChange,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Blend mode selector and layer actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.weight(1.2f)) {
                            OutlinedButton(
                                onClick = { showBlendMenu = true },
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                    horizontal = 6.dp,
                                    vertical = 4.dp
                                )
                            ) {
                                Text(
                                    text = layer.blendMode.name.lowercase().replaceFirstChar { it.uppercase() },
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                            DropdownMenu(
                                expanded = showBlendMenu,
                                onDismissRequest = { showBlendMenu = false }
                            ) {
                                BlendMode.entries.forEach { mode ->
                                    DropdownMenuItem(
                                        text = { Text(mode.name) },
                                        onClick = {
                                            onBlendModeChange(mode)
                                            showBlendMenu = false
                                        }
                                    )
                                }
                            }
                        }

                        OutlinedButton(
                            onClick = onDuplicate,
                            modifier = Modifier.weight(1f),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                horizontal = 4.dp,
                                vertical = 4.dp
                            )
                        ) {
                            Text(
                                text = "Copy",
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }

                        if (canDelete) {
                            OutlinedButton(
                                onClick = onDelete,
                                modifier = Modifier.weight(0.9f),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                    horizontal = 4.dp,
                                    vertical = 4.dp
                                )
                            ) {
                                Text(
                                    text = "Del",
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
