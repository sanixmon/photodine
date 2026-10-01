package dev.photodine.app.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

object PhotodineRoutes {
    const val NEW_CANVAS = "new-canvas"
    const val CANVAS = "canvas"
}

@Composable
fun PhotodineNavHost() {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = PhotodineRoutes.NEW_CANVAS
    ) {
        composable(PhotodineRoutes.NEW_CANVAS) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Button(onClick = { navController.navigate(PhotodineRoutes.CANVAS) }) {
                    Text("Open Canvas (stub)")
                }
            }
        }
        composable(PhotodineRoutes.CANVAS) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Canvas (stub)")
            }
        }
    }
}
