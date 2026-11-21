package com.hybridmusic.player.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.hybridmusic.player.innertube.TrackItem
import com.hybridmusic.player.innertube.UserPlaylist
import com.hybridmusic.player.innertube.YtPlaylistCard
import com.hybridmusic.player.ui.components.LiquidGlassIconButton
import com.hybridmusic.player.ui.components.LiquidGlassPillButton
import com.hybridmusic.player.ui.components.LiquidGlassSurface
import com.hybridmusic.player.ui.components.LiquidSegmentedControl
import com.hybridmusic.player.ui.components.RgbLiveVisualizer
import com.hybridmusic.player.ui.theme.LiquidMotionTokens
import com.hybridmusic.player.ui.theme.LocalRgbPalette
import com.hybridmusic.player.ui.viewmodel.ActiveCollectionView
import com.hybridmusic.player.ui.viewmodel.MainViewModel

@Composable
fun LiquidHomeScreen(
    viewModel: MainViewModel,
    bottomInsetPadding: Dp,
    onOpenSleepTimer: () -> Unit,
    onRequestAddToPlaylist: (TrackItem) -> Unit
) {
    val palette = LocalRgbPalette.current
    val homeFeed by viewModel.homeFeed.collectAsState()
    val selectedMoodIndex by viewModel.selectedMoodIndex.collectAsState()
    val isLoadingHome by viewModel.isLoadingHome.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()
    val likedTracks by viewModel.likedTracks.collectAsState()
    val autoplayRadio by viewModel.autoplayRadioEnabled.collectAsState()
    val sleepSec by viewModel.sleepTimerRemainingSec.collectAsState()

    val likedIds = remember(likedTracks) { likedTracks.map { it.videoId }.toSet() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 12.dp, bottom = bottomInsetPadding),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Top Header แบบ Liquid Glass
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "PlayZongX",
                        style = MaterialTheme.typography.displaySmall.copy(
                            brush = Brush.horizontalGradient(listOf(palette.primary, palette.secondary))
                        )
                    )
                    Text(
                        text = "Liquid Glass • RGB Spectrum Audio",
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.textSecondary
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // ปุ่มเปิด/ปิดวิทยุต่อเนื่อง
                    LiquidGlassSurface(
                        shape = CircleShape,
                        elevated = autoplayRadio,
                        accentGlow = autoplayRadio,
                        onClick = { viewModel.toggleAutoplayRadio() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Radio,
                                contentDescription = "Radio",
                                tint = if (autoplayRadio) palette.primary else palette.textSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (autoplayRadio) "วิทยุ ON" else "วิทยุ OFF",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (autoplayRadio) Color.White else palette.textSecondary
                            )
                        }
                    }

                    LiquidGlassIconButton(
                        icon = Icons.Rounded.Bedtime,
                        contentDescription = "ตั้งเวลาปิดเพลง",
                        isAccent = sleepSec != null,
                        size = 40.dp,
                        onClick = onOpenSleepTimer
                    )
                }
            }
        }

        // Mood Chips ทรงแคปซูลกระจก
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(homeFeed.moods) { index, mood ->
                    val isSelected = index == selectedMoodIndex
                    LiquidGlassSurface(
                        shape = CircleShape,
                        elevated = isSelected,
                        accentGlow = isSelected,
                        onClick = { viewModel.loadHomeFeed(index) }
                    ) {
                        Box(
                            modifier = Modifier
                                .background(
                                    if (isSelected) {
                                        Brush.horizontalGradient(
                                            listOf(
                                                palette.primary.copy(alpha = 0.42f),
                                                palette.secondary.copy(alpha = 0.34f)
                                            )
                                        )
                                    } else {
                                        Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
                                    }
                                )
                                .padding(horizontal = 16.dp, vertical = 9.dp)
                        ) {
                            Text(
                                text = mood.title,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else palette.textSecondary
                            )
                        }
                    }
                }
            }
        }

        if (isLoadingHome && homeFeed.shelves.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = palette.primary)
                }
            }
        } else {
            // Quick Picks 2 คอลัมน์
            if (homeFeed.quickPicks.isNotEmpty()) {
                item {
                    Column(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "เลือกด่วนสำหรับคุณ",
                            style = MaterialTheme.typography.headlineSmall,
                            color = palette.textPrimary
                        )
                        val chunked = homeFeed.quickPicks.take(6).chunked(2)
                        chunked.forEach { rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                rowItems.forEach { track ->
                                    val isCurrent = playbackState.currentTrack?.videoId == track.videoId
                                    LiquidGlassSurface(
                                        shape = RoundedCornerShape(18.dp),
                                        elevated = isCurrent,
                                        accentGlow = isCurrent,
                                        onClick = { viewModel.playTrackOneClick(track, homeFeed.quickPicks) },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            AsyncImage(
                                                model = track.thumbnailUrl,
                                                contentDescription = track.title,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .size(44.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = track.title,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isCurrent) palette.primary else palette.textPrimary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = track.artist,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = palette.textSecondary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            if (isCurrent) {
                                                RgbLiveVisualizer(isPlaying = playbackState.isPlaying)
                                            }
                                        }
                                    }
                                }
                                if (rowItems.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }

            // Shelves
            items(homeFeed.shelves) { shelf ->
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = shelf.title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = palette.textPrimary,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )

                    if (shelf.playlists.isNotEmpty()) {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(shelf.playlists) { card ->
                                LiquidPlaylistTile(
                                    card = card,
                                    onClick = { viewModel.openYtPlaylist(card) }
                                )
                            }
                        }
                    }

                    if (shelf.tracks.isNotEmpty()) {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(shelf.tracks) { track ->
                                val isCurrent = playbackState.currentTrack?.videoId == track.videoId
                                LiquidTrackTile(
                                    track = track,
                                    isCurrent = isCurrent,
                                    isPlaying = isCurrent && playbackState.isPlaying,
                                    onClick = { viewModel.playTrackOneClick(track, shelf.tracks) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LiquidTrackTile(
    track: TrackItem,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    val palette = LocalRgbPalette.current
    LiquidGlassSurface(
        shape = RoundedCornerShape(24.dp),
        elevated = isCurrent,
        accentGlow = isCurrent,
        onClick = onClick,
        modifier = Modifier.width(156.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Box {
                AsyncImage(
                    model = track.thumbnailUrl,
                    contentDescription = track.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(16.dp))
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(listOf(palette.primary, palette.secondary))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isCurrent) {
                        RgbLiveVisualizer(isPlaying = isPlaying)
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = "Play",
                            tint = Color(0xFF04060D),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = track.title,
                style = MaterialTheme.typography.titleMedium,
                color = if (isCurrent) palette.primary else palette.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = track.artist,
                style = MaterialTheme.typography.labelMedium,
                color = palette.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun LiquidPlaylistTile(
    card: YtPlaylistCard,
    onClick: () -> Unit
) {
    val palette = LocalRgbPalette.current
    LiquidGlassSurface(
        shape = RoundedCornerShape(24.dp),
        elevated = false,
        onClick = onClick,
        modifier = Modifier.width(164.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            AsyncImage(
                model = card.thumbnailUrl,
                contentDescription = card.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(16.dp))
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = card.title,
                style = MaterialTheme.typography.titleMedium,
                color = palette.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = card.subtitle,
                style = MaterialTheme.typography.labelMedium,
                color = palette.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun LiquidTrackRow(
    track: TrackItem,
    isCurrent: Boolean,
    isPlaying: Boolean,
    isLiked: Boolean,
    onClick: () -> Unit,
    onToggleLike: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onStartRadio: () -> Unit
) {
    val palette = LocalRgbPalette.current
    var menuExpanded by remember { mutableStateOf(false) }

    LiquidGlassSurface(
        shape = RoundedCornerShape(20.dp),
        elevated = isCurrent,
        accentGlow = isCurrent,
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(contentAlignment = Alignment.Center) {
                AsyncImage(
                    model = track.thumbnailUrl,
                    contentDescription = track.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(14.dp))
                )
                if (isCurrent) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.Black.copy(alpha = 0.55f)),
                        contentAlignment = Alignment.Center
                    ) {
                        RgbLiveVisualizer(isPlaying = isPlaying)
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isCurrent) palette.primary else palette.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = track.artist,
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(
                imageVector = if (isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                contentDescription = "Like",
                tint = if (isLiked) palette.primary else palette.textSecondary,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .clickable { onToggleLike() }
                    .padding(7.dp)
            )

            Box {
                Icon(
                    imageVector = Icons.Rounded.MoreVert,
                    contentDescription = "More",
                    tint = palette.textSecondary,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .clickable { menuExpanded = true }
                        .padding(7.dp)
                )
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    modifier = Modifier.background(Color(0xFF14192B))
                ) {
                    DropdownMenuItem(
                        text = { Text("เปิดวิทยุเพลงแนวนี้", color = Color.White) },
                        leadingIcon = { Icon(Icons.Rounded.Radio, null, tint = palette.primary) },
                        onClick = {
                            menuExpanded = false
                            onStartRadio()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("เพิ่มลงเพลย์ลิสต์", color = Color.White) },
                        leadingIcon = { Icon(Icons.Rounded.PlaylistAdd, null, tint = palette.secondary) },
                        onClick = {
                            menuExpanded = false
                            onAddToPlaylist()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun LiquidSearchScreen(
    viewModel: MainViewModel,
    bottomInsetPadding: Dp,
    onRequestAddToPlaylist: (TrackItem) -> Unit
) {
    val palette = LocalRgbPalette.current
    val query by viewModel.searchQuery.collectAsState()
    val suggestions by viewModel.searchSuggestions.collectAsState()
    val filterIndex by viewModel.searchFilterIndex.collectAsState()
    val songsResult by viewModel.searchSongsResult.collectAsState()
    val playlistsResult by viewModel.searchPlaylistsResult.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()
    val likedTracks by viewModel.likedTracks.collectAsState()
    val likedIds = remember(likedTracks) { likedTracks.map { it.videoId }.toSet() }

    val focusManager = LocalFocusManager.current
    val clipboardManager = LocalClipboardManager.current
    var isFocused by remember { mutableStateOf(false) }

    val searchScale by animateFloatAsState(
        targetValue = if (isFocused) 1.02f else 1f,
        animationSpec = LiquidMotionTokens.SnappySpring,
        label = "search_expand"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 12.dp)
    ) {
        Text(
            text = "ค้นหา & วางลิงก์",
            style = MaterialTheme.typography.displaySmall,
            color = palette.textPrimary,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))

        // ช่องค้นหากระจกขยายตัวเมื่อโฟกัส
        LiquidGlassSurface(
            shape = CircleShape,
            elevated = isFocused,
            accentGlow = isFocused,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .graphicsLayer {
                    scaleX = searchScale
                    scaleY = searchScale
                }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = "Search",
                    tint = if (isFocused) palette.primary else palette.textSecondary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Box(modifier = Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        Text(
                            text = "ชื่อเพลง, ศิลปิน หรือวางลิงก์ YouTube Music...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.textSecondary.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    BasicTextField(
                        value = query,
                        onValueChange = { viewModel.onSearchQueryChange(it) },
                        singleLine = true,
                        textStyle = TextStyle(color = Color.White, fontSize = 15.sp),
                        cursorBrush = SolidColor(palette.primary),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = {
                            focusManager.clearFocus()
                            viewModel.submitSearchOrLink()
                        }),
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { isFocused = it.isFocused }
                    )
                }
                if (query.isNotEmpty()) {
                    Icon(
                        imageVector = Icons.Rounded.Clear,
                        contentDescription = "Clear",
                        tint = palette.textSecondary,
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .clickable { viewModel.onSearchQueryChange("") }
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.ContentPaste,
                        contentDescription = "Paste",
                        tint = palette.primary,
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .clickable {
                                val pasted = clipboardManager.getText()?.text.orEmpty()
                                if (pasted.isNotBlank()) {
                                    focusManager.clearFocus()
                                    viewModel.submitSearchOrLink(pasted)
                                }
                            }
                    )
                }
            }
        }

        // คำแนะนำการค้นหาแบบ Pill
        AnimatedVisibility(visible = suggestions.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(suggestions) { sug ->
                    LiquidGlassSurface(
                        shape = CircleShape,
                        onClick = {
                            focusManager.clearFocus()
                            viewModel.submitSearchOrLink(sug)
                        }
                    ) {
                        Text(
                            text = sug,
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (query.isBlank() && songsResult.isEmpty() && playlistsResult.isEmpty()) {
            Text(
                text = "สำรวจตามแนวเพลง",
                style = MaterialTheme.typography.headlineSmall,
                color = palette.textPrimary,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            )
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 6.dp, bottom = bottomInsetPadding),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(viewModel.browseCategories) { cat ->
                    val catColor = Color(cat.colorHex)
                    LiquidGlassSurface(
                        shape = RoundedCornerShape(22.dp),
                        onClick = { viewModel.selectCategory(cat) }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(86.dp)
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            catColor.copy(alpha = 0.45f),
                                            palette.primary.copy(alpha = 0.15f)
                                        )
                                    )
                                )
                                .padding(14.dp),
                            contentAlignment = Alignment.BottomStart
                        ) {
                            Text(
                                text = cat.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        } else {
            LiquidSegmentedControl(
                options = listOf("เพลง", "เพลย์ลิสต์", "อัลบั้ม"),
                selectedIndex = filterIndex,
                onSelect = { viewModel.setSearchFilter(it) },
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (isSearching) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = palette.primary)
                }
            } else if (filterIndex == 0) {
                LazyColumn(
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = bottomInsetPadding),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(songsResult) { track ->
                        val isCurrent = playbackState.currentTrack?.videoId == track.videoId
                        LiquidTrackRow(
                            track = track,
                            isCurrent = isCurrent,
                            isPlaying = isCurrent && playbackState.isPlaying,
                            isLiked = likedIds.contains(track.videoId),
                            onClick = { viewModel.playTrackOneClick(track, songsResult) },
                            onToggleLike = { viewModel.toggleLikeTrack(track) },
                            onAddToPlaylist = { onRequestAddToPlaylist(track) },
                            onStartRadio = { viewModel.startRadioForTrack(track) }
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = bottomInsetPadding),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(playlistsResult) { card ->
                        LiquidPlaylistTile(
                            card = card,
                            onClick = { viewModel.openYtPlaylist(card) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LiquidLibraryScreen(
    viewModel: MainViewModel,
    bottomInsetPadding: Dp
) {
    val palette = LocalRgbPalette.current
    val likedTracks by viewModel.likedTracks.collectAsState()
    val recentTracks by viewModel.recentTracks.collectAsState()
    val savedPlaylists by viewModel.savedYtPlaylists.collectAsState()
    val userPlaylists by viewModel.userPlaylists.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = bottomInsetPadding),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "คลังเพลงของคุณ",
                    style = MaterialTheme.typography.displaySmall,
                    color = palette.textPrimary
                )
                LiquidGlassPillButton(
                    text = "สร้างเพลย์ลิสต์",
                    icon = Icons.Rounded.Add,
                    isPrimaryRgb = true,
                    onClick = { showCreateDialog = true }
                )
            }
        }

        // การ์ดเพลงที่ถูกใจ
        item {
            LiquidGlassSurface(
                shape = RoundedCornerShape(24.dp),
                elevated = true,
                accentGlow = true,
                onClick = {
                    viewModel.openLocalCollection(
                        id = "liked",
                        title = "เพลงที่ถูกใจ",
                        subtitle = "${likedTracks.size} เพลง",
                        tracks = likedTracks
                    )
                }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Brush.linearGradient(listOf(palette.primary, palette.secondary))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Favorite, null, tint = Color(0xFF04060D), modifier = Modifier.size(28.dp))
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("เพลงที่ถูกใจ", style = MaterialTheme.typography.titleMedium, color = Color.White)
                        Text("${likedTracks.size} เพลงโปรด", style = MaterialTheme.typography.labelMedium, color = palette.textSecondary)
                    }
                }
            }
        }

        // การ์ดประวัติการฟังล่าสุด
        item {
            LiquidGlassSurface(
                shape = RoundedCornerShape(24.dp),
                onClick = {
                    viewModel.openLocalCollection(
                        id = "recent",
                        title = "เล่นล่าสุด",
                        subtitle = "${recentTracks.size} เพลง",
                        tracks = recentTracks
                    )
                }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.History, null, tint = palette.primary, modifier = Modifier.size(28.dp))
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("ประวัติการฟังล่าสุด", style = MaterialTheme.typography.titleMedium, color = Color.White)
                        Text("${recentTracks.size} เพลงที่เล่นไป", style = MaterialTheme.typography.labelMedium, color = palette.textSecondary)
                    }
                }
            }
        }

        if (userPlaylists.isNotEmpty()) {
            item {
                Text("เพลย์ลิสต์ที่สร้างเอง", style = MaterialTheme.typography.headlineSmall, color = Color.White)
            }
            items(userPlaylists) { pl ->
                LiquidGlassSurface(
                    shape = RoundedCornerShape(20.dp),
                    onClick = {
                        viewModel.openLocalCollection(
                            id = pl.id,
                            title = pl.name,
                            subtitle = "${pl.tracks.size} เพลง",
                            tracks = pl.tracks
                        )
                    }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(palette.secondary.copy(alpha = 0.28f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.QueueMusic, null, tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(pl.name, style = MaterialTheme.typography.titleMedium, color = Color.White)
                            Text("${pl.tracks.size} เพลง", style = MaterialTheme.typography.labelMedium, color = palette.textSecondary)
                        }
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = "Delete",
                            tint = palette.textSecondary,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .clickable { viewModel.deleteUserPlaylist(pl.id) }
                                .padding(7.dp)
                        )
                    }
                }
            }
        }

        if (savedPlaylists.isNotEmpty()) {
            item {
                Text("เพลย์ลิสต์ YouTube Music ที่บันทึกไว้", style = MaterialTheme.typography.headlineSmall, color = Color.White)
            }
            items(savedPlaylists) { card ->
                LiquidGlassSurface(
                    shape = RoundedCornerShape(20.dp),
                    onClick = { viewModel.openYtPlaylist(card) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = card.thumbnailUrl,
                            contentDescription = card.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(14.dp))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(card.title, style = MaterialTheme.typography.titleMedium, color = Color.White, maxLines = 1)
                            Text(card.subtitle, style = MaterialTheme.typography.labelMedium, color = palette.textSecondary, maxLines = 1)
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreatePlaylistGlassDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { name ->
                viewModel.createPlaylist(name)
                showCreateDialog = false
            }
        )
    }
}

@Composable
fun LiquidCollectionOverlay(
    collection: ActiveCollectionView,
    viewModel: MainViewModel,
    bottomInsetPadding: Dp,
    onRequestAddToPlaylist: (TrackItem) -> Unit
) {
    val palette = LocalRgbPalette.current
    val playbackState by viewModel.playbackState.collectAsState()
    val likedTracks by viewModel.likedTracks.collectAsState()
    val savedPlaylists by viewModel.savedYtPlaylists.collectAsState()
    val likedIds = remember(likedTracks) { likedTracks.map { it.videoId }.toSet() }
    val isSavedYt = collection.ytCard != null && savedPlaylists.any { it.browseId == collection.ytCard.browseId }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = bottomInsetPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LiquidGlassIconButton(
                    icon = Icons.Rounded.ArrowBack,
                    contentDescription = "Back",
                    onClick = { viewModel.closeCollection() }
                )
                if (collection.ytCard != null) {
                    LiquidGlassIconButton(
                        icon = if (isSavedYt) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                        contentDescription = "Save Playlist",
                        isAccent = isSavedYt,
                        onClick = { viewModel.toggleSaveYtPlaylist(collection.ytCard) }
                    )
                }
            }
        }

        item {
            LiquidGlassSurface(
                shape = RoundedCornerShape(28.dp),
                elevated = true,
                accentGlow = true
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (collection.thumbnailUrl.isNotBlank()) {
                        AsyncImage(
                            model = collection.thumbnailUrl,
                            contentDescription = collection.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(150.dp)
                                .clip(RoundedCornerShape(20.dp))
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                    }
                    Text(
                        text = collection.title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        maxLines = 2
                    )
                    Text(
                        text = collection.subtitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.textSecondary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        LiquidGlassPillButton(
                            text = "เล่นทั้งหมด",
                            icon = Icons.Rounded.PlayArrow,
                            isPrimaryRgb = true,
                            onClick = {
                                collection.tracks.firstOrNull()?.let {
                                    viewModel.playTrackOneClick(it, collection.tracks)
                                }
                            }
                        )
                        LiquidGlassPillButton(
                            text = "สุ่มเล่น",
                            icon = Icons.Rounded.Shuffle,
                            isPrimaryRgb = false,
                            onClick = { viewModel.shufflePlayList(collection.tracks) }
                        )
                    }
                }
            }
        }

        if (collection.isLoading) {
            item {
                Box(modifier = Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = palette.primary)
                }
            }
        } else {
            items(collection.tracks) { track ->
                val isCurrent = playbackState.currentTrack?.videoId == track.videoId
                LiquidTrackRow(
                    track = track,
                    isCurrent = isCurrent,
                    isPlaying = isCurrent && playbackState.isPlaying,
                    isLiked = likedIds.contains(track.videoId),
                    onClick = { viewModel.playTrackOneClick(track, collection.tracks) },
                    onToggleLike = { viewModel.toggleLikeTrack(track) },
                    onAddToPlaylist = { onRequestAddToPlaylist(track) },
                    onStartRadio = { viewModel.startRadioForTrack(track) }
                )
            }
        }
    }
}

@Composable
fun CreatePlaylistGlassDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    val palette = LocalRgbPalette.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF13182B),
        title = { Text("สร้างเพลย์ลิสต์ใหม่", color = Color.White) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                placeholder = { Text("ตั้งชื่อเพลย์ลิสต์...", color = palette.textSecondary) }
            )
        },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank()) onConfirm(name.trim()) }) {
                Text("สร้าง", color = palette.primary, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("ยกเลิก", color = palette.textSecondary)
            }
        }
    )
}

@Composable
fun AddToPlaylistGlassDialog(
    track: TrackItem,
    userPlaylists: List<UserPlaylist>,
    onDismiss: () -> Unit,
    onSelectPlaylist: (String) -> Unit,
    onCreateAndAdd: (String) -> Unit
) {
    val palette = LocalRgbPalette.current
    var newName by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF13182B),
        title = { Text("เพิ่มลงเพลย์ลิสต์", color = Color.White) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                userPlaylists.forEach { pl ->
                    LiquidGlassSurface(
                        shape = RoundedCornerShape(14.dp),
                        onClick = { onSelectPlaylist(pl.id) }
                    ) {
                        Text(
                            text = "${pl.name} (${pl.tracks.size})",
                            color = Color.White,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        )
                    }
                }
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    singleLine = true,
                    placeholder = { Text("หรือพิมพ์ชื่อเพลย์ลิสต์ใหม่...", color = palette.textSecondary) }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { if (newName.isNotBlank()) onCreateAndAdd(newName.trim()) }) {
                Text("สร้างและเพิ่ม", color = palette.primary, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("ปิด", color = palette.textSecondary)
            }
        }
    )
}

@Composable
fun SleepTimerGlassDialog(
    remainingSec: Int?,
    onDismiss: () -> Unit,
    onSelectMinutes: (Int) -> Unit
) {
    val palette = LocalRgbPalette.current
    val presets = listOf(15, 30, 45, 60)
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF13182B),
        title = { Text("ตั้งเวลาปิดเพลงอัตโนมัติ", color = Color.White) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (remainingSec != null) {
                    Text(
                        text = "เหลือเวลาอีก ${remainingSec / 60} นาที ${remainingSec % 60} วินาที",
                        color = palette.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                presets.forEach { mins ->
                    LiquidGlassSurface(
                        shape = RoundedCornerShape(14.dp),
                        onClick = {
                            onSelectMinutes(mins)
                            onDismiss()
                        }
                    ) {
                        Text(
                            text = "$mins นาที",
                            color = Color.White,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (remainingSec != null) {
                TextButton(onClick = {
                    onSelectMinutes(0)
                    onDismiss()
                }) {
                    Text("ยกเลิกตัวตั้งเวลา", color = Color(0xFFF87171))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("ปิด", color = palette.textSecondary)
            }
        }
    )
}
