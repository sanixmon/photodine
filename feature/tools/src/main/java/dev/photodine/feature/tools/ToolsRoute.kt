package dev.photodine.feature.tools

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ThreedRotation
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun ToolsRoute(
    modifier: Modifier = Modifier,
    onColorSwatchClicked: (() -> Unit)? = null,
    onOpenGalleryClicked: (() -> Unit)? = null,
    onExportClicked: (() -> Unit)? = null,
    onLayersToggleClicked: (() -> Unit)? = null,
    onNewPresetSelected: ((width: Int, height: Int) -> Unit)? = null,
    onDuplicateLayerClicked: (() -> Unit)? = null,
    onDeleteLayerClicked: (() -> Unit)? = null,
    viewModel: ToolsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    PixelLabBottomController(
        state = state,
        onIntent = viewModel::onIntent,
        onColorSwatchClicked = onColorSwatchClicked,
        onOpenGalleryClicked = onOpenGalleryClicked,
        onExportClicked = onExportClicked,
        onLayersToggleClicked = onLayersToggleClicked,
        onNewPresetSelected = onNewPresetSelected,
        onDuplicateLayerClicked = onDuplicateLayerClicked,
        onDeleteLayerClicked = onDeleteLayerClicked,
        modifier = modifier
    )
}

@Composable
fun PixelLabBottomController(
    state: ToolsState,
    onIntent: (ToolsIntent) -> Unit,
    onColorSwatchClicked: (() -> Unit)? = null,
    onOpenGalleryClicked: (() -> Unit)? = null,
    onExportClicked: (() -> Unit)? = null,
    onLayersToggleClicked: (() -> Unit)? = null,
    onNewPresetSelected: ((width: Int, height: Int) -> Unit)? = null,
    onDuplicateLayerClicked: (() -> Unit)? = null,
    onDeleteLayerClicked: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Tier 1: Sub-Inspector Drawer (PixelLab parameter sliders with ✓ Done button)
            AnimatedVisibility(visible = state.isOptionsVisible) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
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
                                        text = "Eraser: ${state.eraserSize.toInt()}px",
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
                                    text = "Transform: 1-finger drag translates, 2-finger pinch scales/rotates.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }

                            ActiveTool.CROP -> {
                                Text(
                                    text = "Crop: drag handles on canvas, tap Confirm ✓ or Cancel ✗.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { onIntent(ToolsIntent.ToggleOptions) }) {
                                Text("✓ Done")
                            }
                        }
                    }
                }
            }

            // Tier 2: Horizontal Action Strip based on Selected Tab (PixelLab circular action buttons)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                when (state.selectedTab) {
                    PixelLabTab.PRESETS -> {
                        PixelLabActionChip(label = "1:1") {
                            onNewPresetSelected?.invoke(1080, 1080)
                        }
                        PixelLabActionChip(label = "9:16") {
                            onNewPresetSelected?.invoke(1080, 1920)
                        }
                        PixelLabActionChip(label = "16:9") {
                            onNewPresetSelected?.invoke(1920, 1080)
                        }
                        PixelLabActionChip(label = "2048") {
                            onNewPresetSelected?.invoke(2048, 2048)
                        }
                    }

                    PixelLabTab.DRAW -> {
                        PixelLabActionChip(
                            icon = Icons.Filled.Edit,
                            label = "Brush",
                            isSelected = state.activeTool == ActiveTool.BRUSH
                        ) {
                            onIntent(ToolsIntent.SelectTool(ActiveTool.BRUSH))
                            if (!state.isOptionsVisible) onIntent(ToolsIntent.ToggleOptions)
                        }
                        PixelLabActionChip(
                            icon = Icons.Filled.Delete,
                            label = "Eraser",
                            isSelected = state.activeTool == ActiveTool.ERASER
                        ) {
                            onIntent(ToolsIntent.SelectTool(ActiveTool.ERASER))
                            if (!state.isOptionsVisible) onIntent(ToolsIntent.ToggleOptions)
                        }
                        PixelLabActionChip(icon = Icons.Filled.ColorLens, label = "Color") {
                            onColorSwatchClicked?.invoke()
                        }
                        PixelLabActionChip(icon = Icons.Filled.MoreVert, label = "Options") {
                            onIntent(ToolsIntent.ToggleOptions)
                        }
                    }

                    PixelLabTab.OBJECT -> {
                        PixelLabActionChip(
                            icon = Icons.Filled.ThreedRotation,
                            label = "Move",
                            isSelected = state.activeTool == ActiveTool.MOVE
                        ) {
                            onIntent(ToolsIntent.SelectTool(ActiveTool.MOVE))
                            if (!state.isOptionsVisible) onIntent(ToolsIntent.ToggleOptions)
                        }
                        PixelLabActionChip(icon = Icons.Filled.ContentCopy, label = "Copy") {
                            onDuplicateLayerClicked?.invoke()
                        }
                        PixelLabActionChip(icon = Icons.Filled.Delete, label = "Delete") {
                            onDeleteLayerClicked?.invoke()
                        }
                        PixelLabActionChip(icon = Icons.Filled.Layers, label = "Layers") {
                            onLayersToggleClicked?.invoke()
                        }
                    }

                    PixelLabTab.CANVAS -> {
                        PixelLabActionChip(
                            icon = Icons.Filled.Crop,
                            label = "Crop",
                            isSelected = state.activeTool == ActiveTool.CROP
                        ) {
                            onIntent(ToolsIntent.SelectTool(ActiveTool.CROP))
                        }
                        PixelLabActionChip(icon = Icons.Filled.Image, label = "+ Photo") {
                            onOpenGalleryClicked?.invoke()
                        }
                        PixelLabActionChip(icon = Icons.Filled.Share, label = "Export") {
                            onExportClicked?.invoke()
                        }
                        PixelLabActionChip(icon = Icons.Filled.Layers, label = "Layers") {
                            onLayersToggleClicked?.invoke()
                        }
                    }

                    PixelLabTab.COLOR -> {
                        PixelLabActionChip(icon = Icons.Filled.Palette, label = "Wheel") {
                            onColorSwatchClicked?.invoke()
                        }
                        // Quick color swatches
                        listOf(
                            0xFF000000.toInt(),
                            0xFFFFFFFF.toInt(),
                            0xFFFF1744.toInt(),
                            0xFF2979FF.toInt(),
                            0xFF00E676.toInt(),
                            0xFFFFEA00.toInt()
                        ).forEach { color ->
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(color))
                                    .border(
                                        width = 2.dp,
                                        color = if (state.brushColor == color) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            Color.Gray
                                        },
                                        shape = CircleShape
                                    )
                                    .clickable { onIntent(ToolsIntent.SetBrushColor(color)) }
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

            // Tier 3: PixelLab 5 Category Navigation Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                PixelLabTab.entries.forEach { tab ->
                    val isSelected = state.selectedTab == tab
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onIntent(ToolsIntent.SelectTab(tab)) }
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.label,
                            tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = tab.label,
                            fontSize = 11.sp,
                            maxLines = 1,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PixelLabActionChip(
    label: String,
    icon: ImageVector? = null,
    isSelected: Boolean = false,
    onClick: () -> Unit
) {
    FilledTonalButton(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        shape = RoundedCornerShape(20.dp),
        colors = if (isSelected) {
            ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        } else {
            ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = label,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
