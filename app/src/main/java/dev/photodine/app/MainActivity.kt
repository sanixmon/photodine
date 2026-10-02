package dev.photodine.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dagger.hilt.android.AndroidEntryPoint
import dev.photodine.app.pixellab.PixelLabEditorScreen
import dev.photodine.app.pixellab.PixelLabTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PixelLabTheme {
                PixelLabEditorScreen()
            }
        }
    }
}
