package com.hybridmusic.player

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hybridmusic.player.ui.screens.MainScreen
import com.hybridmusic.player.ui.theme.HybridMusicTheme
import com.hybridmusic.player.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)
        val container = (application as HybridMusicApp).container

        setContent {
            val viewModel: MainViewModel = viewModel(
                factory = MainViewModel.Factory(
                    innerTubeClient = container.innerTubeClient,
                    playerController = container.playerController
                )
            )
            HybridMusicTheme {
                MainScreen(viewModel = viewModel)
            }
        }
    }
}
