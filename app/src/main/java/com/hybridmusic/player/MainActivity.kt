package com.hybridmusic.player

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.hybridmusic.player.ui.PlayZongXRoot
import com.hybridmusic.player.ui.theme.LiquidGlassRgbTheme
import com.hybridmusic.player.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)

        val app = application as HybridMusicApp
        val factory = MainViewModel.Factory(
            innerTubeClient = app.container.innerTubeClient,
            libraryRepository = app.container.libraryRepository,
            playerController = app.container.playerController
        )
        val viewModel = ViewModelProvider(this, factory)[MainViewModel::class.java]

        setContent {
            LiquidGlassRgbTheme {
                PlayZongXRoot(viewModel = viewModel)
            }
        }
    }
}
