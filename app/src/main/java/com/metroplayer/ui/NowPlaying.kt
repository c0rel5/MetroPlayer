package com.metroplayer.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import com.metroplayer.MetroApp
import com.metroplayer.player.PlayerState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun EmptyHint(text: String) {
    Box(Modifier.fillMaxWidth().padding(24.dp)) {
        MText(text, 18, color = Metro.colors.subtle)
    }
}

@Composable
fun NowPlayingScreen(app: MetroApp, push: (Screen) -> Unit, onAddToPlaylist: (Long) -> Unit) {
    val st = app.player.state
    val store = app.store
    val pager = rememberPagerState(pageCount = { 4 })
    val scope = rememberCoroutineScope()
    var menu by remember { mutableStateOf(false) }
    var pos by remember { mutableLongStateOf(0L) }

    LaunchedEffect(st.isPlaying, st.current?.id) {
        pos = app.player.positionMs()
        while (st.isPlaying) {
            delay(250)
            pos = app.player.positionMs()
        }
    }

    val cur = st.current
    val items = buildList {
        if (cur != null) {
            add(MenuEntry(if (store.isFavorite(cur.id)) "убрать из избранного" else "в избранное") {
                store.toggleFavorite(cur.id)
            })
            add(MenuEntry("в плейлист…") { onAddToPlaylist(cur.id) })
        }
        add(MenuEntry(if (st.shuffle) "перемешать: вкл" else "перемешать: выкл") { app.player.toggleShuffle() })
        add(
            MenuEntry(
                when (st.repeatMode) {
                    Player.REPEAT_MODE_ALL -> "повтор: все"
                    Player.REPEAT_MODE_ONE -> "повтор: один"
                    else -> "повтор: выкл"
                }
            ) { app.player.cycleRepeat() }
        )
        add(MenuEntry(if (store.nowMode == 1) "вид: текст" else "вид: обложка") {
            store.chooseNowMode(1 - store.nowMode)
        })
        add(MenuEntry("скорость: ${"%.2f".format(java.util.Locale.US, st.speed).trimEnd('0').trimEnd('.')}×") { app.player.cycleSpeed() })
        add(
            MenuEntry(
                if (app.player.sleepMinutes == 0) "таймер сна: выкл" else "таймер сна: ${app.player.sleepMinutes} мин"
            ) { app.player.cycleSleep() }
        )
        add(MenuEntry("настройки") { push(Screen.Settings) })
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            MetroPivot(
                state = pager,
                caption = "МУЗЫКА",
                titles = listOf("сейчас играет", "очередь", "история", "текст"),
                modifier = Modifier.weight(1f)
            ) { page ->
                when (page) {
                    0 -> NowPage(
                        app, st, pos,
                        onMore = { menu = true },
                        onSeek = { ms ->
                            app.player.seekTo(ms)
                            pos = ms
                        },
                        onOpenLyrics = { scope.launch { pager.animateScrollToPage(3) } }
                    )
                    1 -> QueuePage(app, st)
                    2 -> HistoryPage(app)
                    else -> LyricsPage(app, st, pos)
                }
            }
            AnimatedVisibility(
                visible = pager.targetPage != 0,
                enter = slideInVertically { it },
                exit = slideOutVertically { it }
            ) {
                MetroBottomBar(onMore = { menu = true }) { PlayerButtons(app) }
            }
        }
        MetroMenu(menu, items) { menu = false }
    }
}

@Composable
private fun NowPage(
    app: MetroApp,
    st: PlayerState,
    pos: Long,
    onMore: () -> Unit,
    onSeek: (Long) -> Unit,
    onOpenLyrics: () -> Unit
) {
    val c = Metro.colors
    val t = st.current
    val lyrics = rememberLyrics(app, t?.id)
    val lyricsPos = pos + (t?.let { app.lyrics.offset(it.id) } ?: 0L)
    val showLyrics = app.store.nowMode == 1
    val toggleView = { app.store.chooseNowMode(1 - app.store.nowMode) }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val cover = (maxHeight - 300.dp).coerceIn(140.dp, maxWidth - 48.dp)
        Column(
            Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
        ) {
            Spacer(Modifier.height(4.dp))
            if (showLyrics) {
                LyricsCoverPanel(
                    lyrics, lyricsPos, onToggle = toggleView, onOpen = onOpenLyrics,
                    modifier = Modifier.size(cover).align(Alignment.CenterHorizontally)
                )
            } else {
                Cover(
                    t,
                    Modifier
                        .size(cover)
                        .align(Alignment.CenterHorizontally)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { toggleView() }
                )
            }
            MText(t?.title ?: "ничего не играет", 30, Modifier.padding(top = 20.dp), maxLines = 1)
            MText(
                if (t != null) "${t.artist} · ${t.album}" else "выберите музыку в коллекции",
                16, Modifier.padding(top = 2.dp), color = c.accent, maxLines = 1
            )
            SeekBar(pos, st.durationMs, onSeek, Modifier.padding(top = 12.dp))
            Row(Modifier.fillMaxWidth().offset(y = (-8).dp), horizontalArrangement = Arrangement.SpaceBetween) {
                MText(fmt(pos), 12, color = c.subtle)
                MText(fmt(st.durationMs), 12, color = c.subtle)
            }
            Row(
                Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(28.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MetroCircleButton(56.dp, Ic.PREV, 24.dp, c.fg) { app.player.previous() }
                MetroCircleButton(72.dp, if (st.isPlaying) Ic.PAUSE else Ic.PLAY, 30.dp, c.fg) { app.player.togglePlay() }
                MetroCircleButton(56.dp, Ic.NEXT, 24.dp, c.fg) { app.player.next() }
            }
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                if (!showLyrics) LyricsStrip(lyrics, lyricsPos, onOpenLyrics)
            }
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                Ellipsis(c.subtle, onMore)
            }
        }
    }
}

@Composable
private fun QueuePage(app: MetroApp, st: PlayerState) {
    val offset = st.currentIndex.coerceAtLeast(0)
    val upcoming = st.queue.drop(offset)
    if (upcoming.isEmpty()) {
        EmptyHint("очередь пуста")
        return
    }
    LazyColumn(Modifier.fillMaxSize()) {
        itemsIndexed(upcoming) { i, t ->
            TrackRow(
                title = t.title,
                subtitle = "${t.artist} · ${fmt(t.durationMs)}",
                highlighted = i == 0 && st.currentIndex >= 0,
                onClick = { app.player.seekToIndex(offset + i) }
            )
        }
    }
}

@Composable
private fun HistoryPage(app: MetroApp) {
    val tracks = app.store.history.mapNotNull { app.repo.find(it) }
    if (tracks.isEmpty()) {
        EmptyHint("история пока пуста")
        return
    }
    LazyColumn(Modifier.fillMaxSize()) {
        itemsIndexed(tracks) { i, t ->
            TrackRow(
                title = t.title,
                subtitle = "${t.artist} · ${fmt(t.durationMs)}",
                highlighted = false,
                onClick = { app.player.playTracks(tracks, i) }
            )
        }
    }
}
