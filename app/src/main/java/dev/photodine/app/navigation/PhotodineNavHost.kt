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
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dev.photodine.feature.canvas.CanvasRoute
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
import kotlinx.coroutines.launch

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
    layersViewModel: LayersViewModel = hiltViewModel()
) {
    val toolsState by toolsViewModel.state.collectAsState()
    val layersState by layersViewModel.state.collectAsState()

    var showExportSheet by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }
    var showLayersPanel by remember { mutableStateOf(false) }
    var showAddMenu by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                val bitmap = ImageImporter.decodeBitmap(context, uri)
                if (bitmap != null) {
                    layersViewModel.onIntent(LayerIntent.AddLayer("Photo"))
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
                    text = { Text("🖼 From Gallery") },
                    onClick = {
                        showAddMenu = false
                        galleryLauncher.launch("image/*")
                    }
                )
                DropdownMenuItem(
                    text = { Text("📄 Add Blank Layer") },
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
                    text = { Text("📐 1:1 Square (1080×1080)") },
                    onClick = {
                        showOverflowMenu = false
                        navController.navigate(PhotodineRoutes.canvasRoute(1080, 1080))
                    }
                )
                DropdownMenuItem(
                    text = { Text("📱 9:16 Story (1080×1920)") },
                    onClick = {
                        showOverflowMenu = false
                        navController.navigate(PhotodineRoutes.canvasRoute(1080, 1920))
                    }
                )
                DropdownMenuItem(
                    text = { Text("🖼 16:9 Landscape (1920×1080)") },
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
                    onEyedropperClicked = { showColorPicker = false }
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
            // Left: Add (+), Save/Export (💾)
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onAddClicked) {
                    Text("+", fontSize = 24.sp, color = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onExportClicked) {
                    Text("💾", fontSize = 18.sp)
                }
            }

            // Center: App title
            Text(
                text = "Photodine",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            // Right: Crop (✂), Layers (⧉), Overflow (⋮)
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onCropClicked) {
                    Text("✂", fontSize = 16.sp)
                }
                IconButton(onClick = onLayersToggle) {
                    Text("⧉", fontSize = 20.sp, color = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onOverflowClicked) {
                    Text("⋮", fontSize = 20.sp)
                }
            }
        }
    }
}
