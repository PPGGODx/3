package com.hybridmusic.player

import android.Manifest
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hybridmusic.player.ui.screens.MainScreen
import com.hybridmusic.player.ui.theme.HybridMusicTheme
import com.hybridmusic.player.ui.viewmodel.MainViewModel
import kotlinx.coroutines.delay
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        runCatching {
            enableEdgeToEdge(
                statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
                navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
            )
        }

        val crashFile = File(filesDir, "last_crash.txt")
        val app = application as HybridMusicApp
        val containerResult = runCatching { app.container }
        val initError = containerResult.exceptionOrNull()?.stackTraceToString()
        val previousCrashLog = initError ?: if (crashFile.exists()) {
            runCatching { crashFile.readText() }.getOrNull()
        } else null

        setContent {
            var crashReport by remember { mutableStateOf(previousCrashLog) }
            val container = containerResult.getOrNull()

            if (crashReport != null || container == null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(androidx.compose.ui.graphics.Color(0xFF12060A))
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "ตรวจพบสาเหตุที่แอปปิดตัวลง:",
                        color = androidx.compose.ui.graphics.Color(0xFFFF4D6D),
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = {
                        crashFile.delete()
                        crashReport = null
                    }) {
                        Text("ล้างรายงานและเข้าแอปต่อ")
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = crashReport ?: "ไม่สามารถเริ่มต้นระบบแอปได้",
                        color = androidx.compose.ui.graphics.Color.White,
                        fontSize = 11.sp
                    )
                }
            } else {
                val viewModel: MainViewModel = viewModel(
                    factory = MainViewModel.Factory(
                        repository = container.musicRepository,
                        playerController = container.playerController,
                        driveSyncManager = container.driveSyncManager,
                        networkMonitor = container.networkMonitor
                    )
                )

                val permissionsLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) {
                    viewModel.scanZongFolder(showBanner = false)
                }

                LaunchedEffect(Unit) {
                    delay(350L)
                    val perms = mutableListOf<String>()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        perms.add(Manifest.permission.POST_NOTIFICATIONS)
                        perms.add(Manifest.permission.READ_MEDIA_AUDIO)
                    } else {
                        perms.add(Manifest.permission.READ_EXTERNAL_STORAGE)
                        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                            perms.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        }
                    }
                    runCatching {
                        permissionsLauncher.launch(perms.toTypedArray())
                    }
                    viewModel.scanZongFolder(showBanner = false)
                }

                HybridMusicTheme {
                    MainScreen(viewModel = viewModel)
                }
            }
        }
    }
}
