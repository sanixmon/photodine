package dev.photodine.app.navigation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dev.photodine.feature.canvas.CanvasRoute
import dev.photodine.feature.canvas.NewCanvasRoute
import dev.photodine.feature.layers.LayersRoute

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
            Box(modifier = Modifier.fillMaxSize()) {
                CanvasRoute(
                    width = backStackEntry.arguments?.getInt("width") ?: 1080,
                    height = backStackEntry.arguments?.getInt("height") ?: 1080
                )
                LayersRoute(modifier = Modifier.align(Alignment.BottomCenter))
            }
        }
    }
}
