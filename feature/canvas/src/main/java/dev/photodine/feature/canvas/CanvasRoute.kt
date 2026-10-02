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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import kotlinx.coroutines.launch

/**
 * Canvas screen: [CanvasTextureView] rendering the engine composite.
 * Gestures update MVI state; the view forwards every transform to the
 * compositor on the GL thread.
 */
@Composable
fun CanvasRoute(
    width: Int,
    height: Int,
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
                val bitmap = ImageImporter.decodeBitmap(context, uri)
                if (bitmap != null) {
                    viewModel.compositor.addLayerFromBitmap(bitmap, "Imported Photo")
                }
                isImporting = false
            }
        }
    }

    LaunchedEffect(width, height) {
        viewModel.onIntent(CanvasViewModel.CanvasIntent.InitCanvas(width, height))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1E1E1E))
    ) {
        AndroidView(
            factory = { ctx ->
                CanvasTextureView(ctx).apply {
                    compositor = viewModel.compositor
                    onTransformChanged = { transform ->
                        viewModel.onIntent(
                            CanvasViewModel.CanvasIntent.TransformChanged(transform)
                        )
                    }
                    onSurfaceAvailable = {
                        viewModel.onIntent(CanvasViewModel.CanvasIntent.SurfaceActive)
                    }
                }
            },
            update = { view ->
                view.updateTransform(state.transform)
            },
            modifier = Modifier.fillMaxSize(),
            onRelease = { view ->
                view.compositor = null
                view.onTransformChanged = null
                view.onSurfaceAvailable = null
            }
        )

        // Top-right action: Import Photo layer
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            if (isImporting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                FilledTonalButton(onClick = { layerImportLauncher.launch("image/*") }) {
                    Text("+ Photo")
                }
            }
        }
    }
}
