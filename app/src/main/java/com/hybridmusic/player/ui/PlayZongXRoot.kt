package com.hybridmusic.player.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.hybridmusic.player.innertube.TrackItem
import com.hybridmusic.player.ui.components.LiquidFloatingNavBar
import com.hybridmusic.player.ui.components.LiquidRgbAuroraBackground
import com.hybridmusic.player.ui.components.LiquidToastBanner
import com.hybridmusic.player.ui.screens.AddToPlaylistGlassDialog
import com.hybridmusic.player.ui.screens.LiquidCollectionOverlay
import com.hybridmusic.player.ui.screens.LiquidFullPlayerSheet
import com.hybridmusic.player.ui.screens.LiquidHomeScreen
import com.hybridmusic.player.ui.screens.LiquidLibraryScreen
import com.hybridmusic.player.ui.screens.LiquidMiniPlayerBar
import com.hybridmusic.player.ui.screens.LiquidSearchScreen
import com.hybridmusic.player.ui.screens.SleepTimerGlassDialog
import com.hybridmusic.player.ui.viewmodel.MainViewModel
import kotlinx.coroutines.delay

@Composable
fun PlayZongXRoot(viewModel: MainViewModel) {
    val selectedTab by viewModel.selectedTab.collectAsState()
    val activeCollection by viewModel.activeCollection.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()
    val isFullPlayerOpen by viewModel.isFullPlayerOpen.collectAsState()
    val statusBanner by viewModel.statusBanner.collectAsState()
    val userPlaylists by viewModel.userPlaylists.collectAsState()
    val sleepSec by viewModel.sleepTimerRemainingSec.collectAsState()

    var trackForPlaylistDialog by remember { mutableStateOf<TrackItem?>(null) }
    var showSleepDialog by remember { mutableStateOf(false) }

    LaunchedEffect(statusBanner) {
        if (!statusBanner.isNullOrBlank()) {
            delay(3200L)
            viewModel.clearStatusBanner()
        }
    }

    BackHandler(enabled = isFullPlayerOpen || activeCollection != null) {
        if (isFullPlayerOpen) {
            viewModel.openFullPlayer(false)
        } else if (activeCollection != null) {
            viewModel.closeCollection()
        }
    }

    val hasMiniPlayer = playbackState.currentTrack != null
    val bottomClearance = if (hasMiniPlayer) 168.dp else 104.dp

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Black
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // พื้นหลังสีดำสนิทพร้อมแสง RGB Aurora
            LiquidRgbAuroraBackground()

            // WebView Engine สำรองแบบโปร่งใส 1dp (ไม่ให้เกิดพื้นหลังสีขาว)
            viewModel.playerController.getEmbedWebView()?.let { webView ->
                AndroidView(
                    factory = {
                        webView.apply {
                            setBackgroundColor(android.graphics.Color.TRANSPARENT)
                        }
                    },
                    modifier = Modifier.size(1.dp)
                )
            }

            // เนื้อหาหลักของแต่ละแท็บหรือหน้าเพลย์ลิสต์
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                val currentCol = activeCollection
                if (currentCol != null) {
                    LiquidCollectionOverlay(
                        collection = currentCol,
                        viewModel = viewModel,
                        bottomInsetPadding = bottomClearance,
                        onRequestAddToPlaylist = { trackForPlaylistDialog = it }
                    )
                } else {
                    when (selectedTab) {
                        0 -> LiquidHomeScreen(
                            viewModel = viewModel,
                            bottomInsetPadding = bottomClearance,
                            onOpenSleepTimer = { showSleepDialog = true },
                            onRequestAddToPlaylist = { trackForPlaylistDialog = it }
                        )
                        1 -> LiquidSearchScreen(
                            viewModel = viewModel,
                            bottomInsetPadding = bottomClearance,
                            onRequestAddToPlaylist = { trackForPlaylistDialog = it }
                        )
                        2 -> LiquidLibraryScreen(
                            viewModel = viewModel,
                            bottomInsetPadding = bottomClearance
                        )
                    }
                }
            }

            // Floating Mini Player + Floating Capsule Nav Bar ด้านล่าง
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (hasMiniPlayer) {
                    LiquidMiniPlayerBar(
                        viewModel = viewModel,
                        onExpand = { viewModel.openFullPlayer(true) }
                    )
                }
                LiquidFloatingNavBar(
                    items = listOf(
                        "หน้าหลัก" to Icons.Rounded.Home,
                        "ค้นหา" to Icons.Rounded.Search,
                        "คลังเพลง" to Icons.Rounded.LibraryMusic
                    ),
                    selectedIndex = selectedTab,
                    onSelect = { viewModel.selectTab(it) }
                )
            }

            // Toast แจ้งเตือนกระจกเลื่อนจากด้านบน
            LiquidToastBanner(
                message = statusBanner,
                onDismiss = { viewModel.clearStatusBanner() },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
            )

            // หน้าจอ Full Player แบบ Liquid Glass
            LiquidFullPlayerSheet(
                viewModel = viewModel,
                onRequestAddToPlaylist = { trackForPlaylistDialog = it }
            )

            // Dialog เพิ่มลงเพลย์ลิสต์ & ตั้งเวลาปิดเพลง
            trackForPlaylistDialog?.let { targetTrack ->
                AddToPlaylistGlassDialog(
                    track = targetTrack,
                    userPlaylists = userPlaylists,
                    onDismiss = { trackForPlaylistDialog = null },
                    onSelectPlaylist = { plId ->
                        viewModel.addTrackToPlaylist(plId, targetTrack)
                        trackForPlaylistDialog = null
                    },
                    onCreateAndAdd = { newName ->
                        viewModel.createPlaylist(newName, targetTrack)
                        trackForPlaylistDialog = null
                    }
                )
            }

            if (showSleepDialog) {
                SleepTimerGlassDialog(
                    remainingSec = sleepSec,
                    onDismiss = { showSleepDialog = false },
                    onSelectMinutes = { viewModel.setSleepTimerMinutes(it) }
                )
            }
        }
    }
}
