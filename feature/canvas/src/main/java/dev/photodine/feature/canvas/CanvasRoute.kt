package dev.photodine.feature.canvas

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import dev.photodine.core.engine.CropRect
import dev.photodine.core.engine.Layer
import dev.photodine.core.engine.LayerTransform
import dev.photodine.feature.canvas.stroke.CatmullRomSpline
import dev.photodine.feature.canvas.stroke.StrokePoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Canvas screen: [CanvasTextureView] rendering the engine composite.
 * Gestures update MVI state; the view forwards every transform to the
 * compositor on the GL thread. Supports drawing strokes, move overlays, and crop.
 */
@Composable
fun CanvasRoute(
    width: Int,
    height: Int,
    activeToolName: String = "BRUSH",
    brushSize: Float = 20f,
    brushOpacity: Float = 1f,
    brushColor: Int = 0xFF000000.toInt(),
    eraserSize: Float = 30f,
    activeLayerId: UUID? = null,
    activeLayerTransform: LayerTransform? = null,
    isEyedropperActive: Boolean = false,
    onPhotoLayerCreated: ((Layer) -> Unit)? = null,
    onCanvasTapped: ((x: Float, y: Float) -> Unit)? = null,
    onConfirmCrop: ((CropRect) -> Unit)? = null,
    onCancelCrop: (() -> Unit)? = null,
    onUndoClicked: (() -> Unit)? = null,
    onRedoClicked: (() -> Unit)? = null,
    onExportClicked: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    viewModel: CanvasViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isImporting by remember { mutableStateOf(false) }

    val layerImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                isImporting = true
                val bitmap = withContext(Dispatchers.IO) {
                    ImageImporter.decodeBitmap(context, uri)
                }
                if (bitmap != null) {
                    val created = withContext(Dispatchers.IO) {
                        viewModel.compositor.addLayerFromBitmap(bitmap, "Imported Photo")
                    }
                    runCatching { bitmap.recycle() }
                    if (created.textureId != 0) {
                        onPhotoLayerCreated?.invoke(created)
                    }
                }
                isImporting = false
            }
        }
    }

    LaunchedEffect(width, height) {
        viewModel.onIntent(CanvasViewModel.CanvasIntent.InitCanvas(width, height))
    }

    val isDrawing = (activeToolName == "BRUSH" || activeToolName == "ERASER") && !isEyedropperActive
    val isEraser = activeToolName == "ERASER"
    val strokeSize = if (isEraser) eraserSize else brushSize

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF1E1E1E))
    ) {
        AndroidView(
            factory = { ctx ->
                CanvasTextureView(ctx).apply {
                    compositor = viewModel.compositor
                    isDrawingTool = isDrawing
                    onTransformChanged = { transform ->
                        viewModel.onIntent(
                            CanvasViewModel.CanvasIntent.TransformChanged(transform)
                        )
                    }
                    onSurfaceAvailable = {
                        viewModel.onIntent(CanvasViewModel.CanvasIntent.SurfaceActive)
                    }
                    onStrokeBatch = { batch ->
                        val targetId = activeLayerId ?: viewModel.compositor.layers.firstOrNull()?.id
                        if (targetId != null) {
                            val stamps = CatmullRomSpline.generateStamps(
                                points = batch,
                                baseSize = strokeSize,
                                opacity = brushOpacity,
                                color = brushColor,
                                isEraser = isEraser
                            )
                            viewModel.compositor.renderStamps(stamps, targetId)
                        }
                    }
                    this.onCanvasTapped = onCanvasTapped
                }
            },
            update = { view ->
                view.updateTransform(state.transform)
                view.isDrawingTool = isDrawing
                view.onCanvasTapped = onCanvasTapped
                view.onStrokeBatch = { batch ->
                    val targetId = activeLayerId ?: viewModel.compositor.layers.firstOrNull()?.id
                    if (targetId != null) {
                        val stamps = CatmullRomSpline.generateStamps(
                            points = batch,
                            baseSize = strokeSize,
                            opacity = brushOpacity,
                            color = brushColor,
                            isEraser = isEraser
                        )
                        viewModel.compositor.renderStamps(stamps, targetId)
                    }
                }
            },
            modifier = Modifier.fillMaxSize(),
            onRelease = { view ->
                view.compositor = null
                view.onTransformChanged = null
                view.onSurfaceAvailable = null
                view.onStrokeBatch = null
                view.onCanvasTapped = null
            }
        )

        // Move Transform Overlay when activeTool == MOVE
        if (activeToolName == "MOVE" && activeLayerTransform != null) {
            MoveTransformOverlay(
                canvasWidth = width,
                canvasHeight = height,
                layerTransform = activeLayerTransform,
                viewTransform = state.transform
            )
        }

        // Crop Overlay when activeTool == CROP
        if (activeToolName == "CROP") {
            CropOverlay(
                canvasWidth = width,
                canvasHeight = height,
                viewTransform = state.transform,
                onConfirmCrop = { rect ->
                    viewModel.compositor.cropCanvas(rect)
                    onConfirmCrop?.invoke(rect)
                },
                onCancelCrop = { onCancelCrop?.invoke() }
            )
        }

        // Top toolbar overlay: Undo, Redo, + Photo, Export
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Top-start: Undo and Redo
            androidx.compose.foundation.layout.Row(
                modifier = Modifier.align(Alignment.TopStart),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp)
            ) {
                FilledTonalButton(
                    onClick = { onUndoClicked?.invoke() },
                    modifier = Modifier.size(36.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                    shape = androidx.compose.foundation.shape.CircleShape
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Undo,
                        contentDescription = "Undo"
                    )
                }
                FilledTonalButton(
                    onClick = { onRedoClicked?.invoke() },
                    modifier = Modifier.size(36.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                    shape = androidx.compose.foundation.shape.CircleShape
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Redo,
                        contentDescription = "Redo"
                    )
                }
            }

            // Top-end: + Photo and Export
            androidx.compose.foundation.layout.Row(
                modifier = Modifier.align(Alignment.TopEnd),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp)
            ) {
                if (isImporting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    FilledTonalButton(
                        onClick = { layerImportLauncher.launch("image/*") },
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            horizontal = 8.dp,
                            vertical = 4.dp
                        )
                    ) {
                        Text(
                            text = "+ Photo",
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1
                        )
                    }
                }

                FilledTonalButton(
                    onClick = { onExportClicked?.invoke() },
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 10.dp,
                        vertical = 4.dp
                    )
                ) {
                    Text(
                        text = "Export",
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
