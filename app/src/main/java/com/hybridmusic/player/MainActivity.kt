package com.hybridmusic.player

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
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
            statusBarStyle = SystemBarStyle.dark(Color.BLACK),
            navigationBarStyle = SystemBarStyle.dark(Color.BLACK)
        )
        super.onCreate(savedInstanceState)

        window.setBackgroundDrawable(ColorDrawable(Color.BLACK))
        window.decorView.setBackgroundColor(Color.BLACK)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.decorView.isForceDarkAllowed = false
        }

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
