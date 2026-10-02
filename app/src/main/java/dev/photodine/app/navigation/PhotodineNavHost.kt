package dev.photodine.app.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dev.photodine.feature.canvas.CanvasRoute
import dev.photodine.feature.canvas.NewCanvasRoute
import dev.photodine.feature.colorpicker.ColorPickerBottomSheet
import dev.photodine.feature.export.ExportBottomSheet
import dev.photodine.feature.layers.LayersRoute
import dev.photodine.feature.layers.LayersViewModel
import dev.photodine.feature.tools.ToolsIntent
import dev.photodine.feature.tools.ToolsRoute
import dev.photodine.feature.tools.ToolsViewModel

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
                height = backStackEntry.arguments?.getInt("height") ?: 1080
            )
        }
    }
}

@Composable
private fun CanvasScreen(
    width: Int,
    height: Int,
    toolsViewModel: ToolsViewModel = hiltViewModel(),
    layersViewModel: LayersViewModel = hiltViewModel()
) {
    val toolsState by toolsViewModel.state.collectAsState()
    val layersState by layersViewModel.state.collectAsState()

    var showExportSheet by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        // Main Canvas
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

        // Bottom panels: Tools row on top, Layers sheet docked below
        Column(
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            ToolsRoute(
                onColorSwatchClicked = { showColorPicker = true }
            )
            LayersRoute()
        }

        // Export dialog
        if (showExportSheet) {
            ExportBottomSheet(onDismissRequest = { showExportSheet = false })
        }

        // Color picker dialog
        if (showColorPicker) {
            ColorPickerBottomSheet(
                onDismissRequest = { showColorPicker = false },
                onColorSelected = { color ->
                    toolsViewModel.onIntent(ToolsIntent.SetBrushColor(color))
                },
                onEyedropperClicked = {
                    showColorPicker = false
                }
            )
        }
    }
}
