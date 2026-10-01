package dev.photopia.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dagger.hilt.android.AndroidEntryPoint
import dev.photopia.app.navigation.PhotopiaNavHost
import dev.photopia.core.ui.theme.PhotopiaTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PhotopiaTheme {
                PhotopiaNavHost()
            }
        }
    }
}
