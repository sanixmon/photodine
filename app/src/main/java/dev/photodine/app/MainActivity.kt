package dev.photodine.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dagger.hilt.android.AndroidEntryPoint
import dev.photodine.app.navigation.PhotodineNavHost
import dev.photodine.core.ui.theme.PhotodineTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PhotodineTheme {
                PhotodineNavHost()
            }
        }
    }
}
