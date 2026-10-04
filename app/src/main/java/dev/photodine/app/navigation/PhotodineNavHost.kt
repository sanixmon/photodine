package dev.photodine.app.navigation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dev.photodine.feature.canvas.CanvasRoute
import dev.photodine.feature.canvas.CanvasViewModel
import dev.photodine.feature.canvas.ImageImporter
import dev.photodine.feature.canvas.NewCanvasRoute
import dev.photodine.feature.colorpicker.ColorPickerBottomSheet
import dev.photodine.feature.export.ExportBottomSheet
import dev.photodine.feature.layers.LayerIntent
import dev.photodine.feature.layers.LayersPanel
import dev.photodine.feature.layers.LayersViewModel
import dev.photodine.feature.tools.ActiveTool
import dev.photodine.feature.tools.ToolsIntent
import dev.photodine.feature.tools.ToolsRoute
import dev.photodine.feature.tools.ToolsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object PhotodineRoutes {
    const val NEW_CANVAS = "new-canvas"
    const val CANVAS = "canvas/{width}/{height}"

    fun canvasRoute(width: Int, height: Int): String = "canvas/$width/$height"
}

@Composable
fun PhotodineNavHost() {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = PhotodineRoutes.NEW_CANVAS
    ) {
        composable(PhotodineRoutes.NEW_CANVAS) {
            NewCanvasRoute(
                onCanvasCreated = { width, height ->
                    navController.navigate(PhotodineRoutes.canvasRoute(width, height))
                }
            )
        }
        composable(
            route = PhotodineRoutes.CANVAS,
            arguments = listOf(
                navArgument("width") { type = NavType.IntType },
                navArgument("height") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            CanvasScreen(
                width = backStackEntry.arguments?.getInt("width") ?: 1080,
                height = backStackEntry.arguments?.getInt("height") ?: 1080,
                navController = navController
            )
        }
    }
}

@Composable
private fun CanvasScreen(
    width: Int,
    height: Int,
    navController: NavController,
    toolsViewModel: ToolsViewModel = hiltViewModel(),
    layersViewModel: LayersViewModel = hiltViewModel(),
    canvasViewModel: CanvasViewModel = hiltViewModel()
) {
    val toolsState by toolsViewModel.state.collectAsState()
    val layersState by layersViewModel.state.collectAsState()

    var showExportSheet by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }
    var showLayersPanel by remember { mutableStateOf(false) }
    var showAddMenu by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    var isEyedropperActive by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                val bitmap = withContext(Dispatchers.IO) {
                    ImageImporter.decodeBitmap(context, uri)
                }
                if (bitmap != null) {
                    val created = withContext(Dispatchers.IO) {
                        canvasViewModel.compositor.addLayerFromBitmap(bitmap, "Photo")
                    }
                    runCatching { bitmap.recycle() }
                    if (created.textureId != 0) {
                        layersViewModel.onIntent(LayerIntent.AddPhotoLayer(created))
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            PixelLabTopBar(
                onAddClicked = { showAddMenu = true },
                onExportClicked = { showExportSheet = true },
                onLayersToggle = { showLayersPanel = !showLayersPanel },
                onCropClicked = {
                    toolsViewModel.onIntent(ToolsIntent.SelectTool(ActiveTool.CROP))
                },
                onOverflowClicked = { showOverflowMenu = true }
            )
        },
        bottomBar = {
            ToolsRoute(
                onColorSwatchClicked = { showColorPicker = true },
                onOpenGalleryClicked = { galleryLauncher.launch("image/*") },
                onExportClicked = { showExportSheet = true },
                onLayersToggleClicked = { showLayersPanel = !showLayersPanel },
                onNewPresetSelected = { w, h ->
                    navController.navigate(PhotodineRoutes.canvasRoute(w, h))
                },
                onDuplicateLayerClicked = {
                    layersState.activeLayerId?.let {
                        layersViewModel.onIntent(LayerIntent.DuplicateLayer(it))
                    }
                },
                onDeleteLayerClicked = {
                    layersState.activeLayerId?.let {
                        layersViewModel.onIntent(LayerIntent.DeleteLayer(it))
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Main Canvas Area
            CanvasRoute(
                width = width,
                height = height,
                activeToolName = toolsState.activeTool.name,
                brushSize = toolsState.brushSize,
                brushOpacity = toolsState.brushOpacity,
                brushColor = toolsState.brushColor,
                eraserSize = toolsState.eraserSize,
                activeLayerId = layersState.activeLayerId,
                activeLayerTransform = layersState.activeLayer?.transform,
                isEyedropperActive = isEyedropperActive,
                onPhotoLayerCreated = { layer ->
                    layersViewModel.onIntent(LayerIntent.AddPhotoLayer(layer))
                },
                onCanvasTapped = { x, y ->
                    if (isEyedropperActive) {
                        coroutineScope.launch {
                            val sampled = withContext(Dispatchers.IO) {
                                canvasViewModel.compositor.readPixel(x.toInt(), y.toInt())
                            }
                            toolsViewModel.onIntent(ToolsIntent.SetBrushColor(sampled))
                            isEyedropperActive = false
                        }
                    }
                },
                onExportClicked = { showExportSheet = true },
                modifier = Modifier.fillMaxSize()
            )

            // Floating Layer Panel (PixelLab style)
            if (showLayersPanel) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .align(Alignment.TopEnd)
                        .padding(top = 8.dp, end = 8.dp)
                ) {
                    LayersPanel(
                        state = layersState.copy(isExpanded = true),
                        onIntent = { intent ->
                            if (intent is LayerIntent.ToggleExpanded) {
                                showLayersPanel = false
                            } else {
                                layersViewModel.onIntent(intent)
                            }
                        }
                    )
                }
            }

            // Quick Add Menu Dropdown
            DropdownMenu(
                expanded = showAddMenu,
                onDismissRequest = { showAddMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("From Gallery") },
                    onClick = {
                        showAddMenu = false
                        galleryLauncher.launch("image/*")
                    }
                )
                DropdownMenuItem(
                    text = { Text("Add Blank Layer") },
                    onClick = {
                        showAddMenu = false
                        layersViewModel.onIntent(LayerIntent.AddLayer())
                    }
                )
            }

            // Top-right Overflow Menu
            DropdownMenu(
                expanded = showOverflowMenu,
                onDismissRequest = { showOverflowMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("1:1 Square (1080×1080)") },
                    onClick = {
                        showOverflowMenu = false
                        navController.navigate(PhotodineRoutes.canvasRoute(1080, 1080))
                    }
                )
                DropdownMenuItem(
                    text = { Text("9:16 Story (1080×1920)") },
                    onClick = {
                        showOverflowMenu = false
                        navController.navigate(PhotodineRoutes.canvasRoute(1080, 1920))
                    }
                )
                DropdownMenuItem(
                    text = { Text("16:9 Landscape (1920×1080)") },
                    onClick = {
                        showOverflowMenu = false
                        navController.navigate(PhotodineRoutes.canvasRoute(1920, 1080))
                    }
                )
            }

            // Export Sheet
            if (showExportSheet) {
                ExportBottomSheet(onDismissRequest = { showExportSheet = false })
            }

            // Color Picker Sheet
            if (showColorPicker) {
                ColorPickerBottomSheet(
                    onDismissRequest = { showColorPicker = false },
                    onColorSelected = { color ->
                        toolsViewModel.onIntent(ToolsIntent.SetBrushColor(color))
                    },
                    onEyedropperClicked = {
                        isEyedropperActive = true
                        showColorPicker = false
                    }
                )
            }
        }
    }
}

@Composable
fun PixelLabTopBar(
    onAddClicked: () -> Unit,
    onExportClicked: () -> Unit,
    onLayersToggle: () -> Unit,
    onCropClicked: () -> Unit,
    onOverflowClicked: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Add, Save/Export
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onAddClicked) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Add layer",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onExportClicked) {
                    Icon(
                        imageVector = Icons.Outlined.Save,
                        contentDescription = "Save and export"
                    )
                }
            }

            // Center: App title
            Text(
                text = "Photodine",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            // Right: Crop, Layers, Overflow
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onCropClicked) {
                    Icon(
                        imageVector = Icons.Filled.Crop,
                        contentDescription = "Crop"
                    )
                }
                IconButton(onClick = onLayersToggle) {
                    Icon(
                        imageVector = Icons.Filled.Layers,
                        contentDescription = "Layers",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onOverflowClicked) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "Canvas presets"
                    )
                }
            }
        }
    }
}
