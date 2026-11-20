package com.hybridmusic.player

import android.content.Intent
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
    private var activeViewModel: MainViewModel? = null

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
                    libraryRepository = container.libraryRepository,
                    playerController = container.playerController
                )
            )
            activeViewModel = viewModel
            HybridMusicTheme {
                MainScreen(viewModel = viewModel)
            }
        }
        handleIncomingIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        val sharedText = when (intent?.action) {
            Intent.ACTION_SEND -> intent.getStringExtra(Intent.EXTRA_TEXT)
            Intent.ACTION_VIEW -> intent.dataString
            else -> null
        }
        if (!sharedText.isNullOrBlank()) {
            activeViewModel?.submitSearchOrLink(sharedText)
        }
    }
}
