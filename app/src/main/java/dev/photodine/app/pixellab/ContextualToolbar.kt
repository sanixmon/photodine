package dev.photodine.app.pixellab

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FlipToBack
import androidx.compose.material.icons.filled.FlipToFront
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InvertColors
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThreedRotation
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ContextualToolbar(
    activeTab: EditorTab,
    selectedId: String?,
    onAddText: () -> Unit,
    onQuoteClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onCopyClick: () -> Unit,
    onBringToFront: () -> Unit,
    onSendToBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(100.dp),
        color = PixelLabToolbarBg
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                thickness = 1.dp
            )

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                when (activeTab) {
                    EditorTab.Text -> {
                        if (selectedId == null) {
                            item {
                                ToolbarItem(
                                    icon = Icons.Default.Add,
                                    label = "text",
                                    onClick = onAddText
                                )
                            }
                            item {
                                ToolbarItem(
                                    icon = Icons.Default.FormatQuote,
                                    label = "quotes",
                                    onClick = onQuoteClick
                                )
                            }
                        } else {
                            item {
                                ToolbarItem(
                                    icon = Icons.Default.Palette,
                                    label = "styles",
                                    onClick = { /* Styles */ }
                                )
                            }
                            item {
                                VerticalDivider(
                                    modifier = Modifier
                                        .height(48.dp)
                                        .padding(horizontal = 4.dp),
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                    thickness = 1.dp
                                )
                            }
                            item {
                                ToolbarItem(
                                    icon = Icons.Default.Edit,
                                    label = "edit",
                                    onClick = onEditClick
                                )
                            }
                            item {
                                ToolbarItem(
                                    icon = Icons.Default.Delete,
                                    label = "delete",
                                    onClick = onDeleteClick
                                )
                            }
                            item {
                                ToolbarItem(
                                    icon = Icons.Default.ContentCopy,
                                    label = "copy",
                                    onClick = onCopyClick
                                )
                            }
                            item {
                                ToolbarItem(
                                    icon = Icons.Default.FlipToFront,
                                    label = "to front",
                                    onClick = onBringToFront
                                )
                            }
                            item {
                                ToolbarItem(
                                    icon = Icons.Default.FlipToBack,
                                    label = "to back",
                                    onClick = onSendToBack
                                )
                            }
                        }
                    }

                    EditorTab.Blend -> {
                        item { ToolbarItem(Icons.Default.InvertColors, "blend") {} }
                        item { ToolbarItem(Icons.Default.Opacity, "opacity") {} }
                        item { ToolbarItem(Icons.Default.ColorLens, "color") {} }
                    }

                    EditorTab.Shapes -> {
                        item { ToolbarItem(Icons.Default.Star, "shapes") {} }
                        item { ToolbarItem(Icons.Default.Crop, "draw") {} }
                        item { ToolbarItem(Icons.Default.Palette, "fill") {} }
                    }

                    EditorTab.Images -> {
                        item { ToolbarItem(Icons.Default.Image, "from gallery") {} }
                        item { ToolbarItem(Icons.Default.Crop, "crop") {} }
                        item { ToolbarItem(Icons.Default.Palette, "filter") {} }
                    }

                    EditorTab.Effects -> {
                        item { ToolbarItem(Icons.Default.AutoFixHigh, "vignette") {} }
                        item { ToolbarItem(Icons.Default.ThreedRotation, "3D rotate") {} }
                        item { ToolbarItem(Icons.Default.Opacity, "shadow") {} }
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolbarItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = PixelLabInactiveGray,
            modifier = Modifier.size(32.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = PixelLabInactiveGray,
            fontSize = 11.sp,
            maxLines = 1
        )
    }
}
