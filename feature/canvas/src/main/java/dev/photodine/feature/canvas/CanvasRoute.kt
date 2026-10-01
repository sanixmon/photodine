package dev.photodine.feature.canvas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel

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

    LaunchedEffect(width, height) {
        viewModel.onIntent(CanvasViewModel.CanvasIntent.InitCanvas(width, height))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1E1E1E))
    ) {
        AndroidView(
            factory = { context ->
                CanvasTextureView(context).apply {
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
    }
}
