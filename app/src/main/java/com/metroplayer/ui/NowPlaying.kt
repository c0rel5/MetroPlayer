package com.metroplayer.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
    var qMenu by remember { mutableStateOf(false) }
    var qIndex by remember { mutableStateOf(-1) }

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

    // меню трека в очереди (долгое нажатие или «···» в строке)
    val qi = qIndex
    val qTrack = st.queue.getOrNull(qi)
    val queueItems = buildList {
        if (qTrack != null) {
            add(MenuEntry("играть") { app.player.seekToIndex(qi) })
            add(MenuEntry(if (store.isFavorite(qTrack.id)) "убрать из избранного" else "в избранное") {
                store.toggleFavorite(qTrack.id)
            })
            add(MenuEntry("в плейлист…") { onAddToPlaylist(qTrack.id) })
            add(MenuEntry("убрать из очереди") { app.player.removeFromQueue(qi) })
        }
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
                    1 -> QueuePage(app, st) { idx ->
                        qIndex = idx
                        qMenu = true
                    }
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
        MetroMenu(qMenu, queueItems, header = qTrack?.title) { qMenu = false }
    }
}

@Composable
private fun RoundIconButton(path: String, color: Color, label: String, filled: Boolean = false, onClick: () -> Unit) {
    Box(
        Modifier
            .size(52.dp)
            .semantics { contentDescription = label }
            .metroPress(pressScale = 0.86f, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        MetroIcon(path, 26.dp, color, filled = filled)
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
    var scrub by remember { mutableStateOf<Long?>(null) }
    val fav = t != null && app.store.isFavorite(t.id)
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val cover = (maxHeight - 330.dp).coerceIn(140.dp, maxWidth - 48.dp)
        Column(
            Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
        ) {
            Spacer(Modifier.height(4.dp))
            // Тап по обложке: карточка переворачивается вокруг вертикальной оси, и на обратной стороне — текст песни
            AnimatedContent(
                targetState = showLyrics,
                modifier = Modifier.size(cover).align(Alignment.CenterHorizontally),
                transitionSpec = { fadeIn(tween(160, delayMillis = 150)) togetherWith fadeOut(tween(120)) },
                label = "coverFlip"
            ) { lyricsOn ->
                val rot by transition.animateFloat(
                    transitionSpec = { tween(320, easing = FastOutSlowInEasing) },
                    label = "flipRot"
                ) { s ->
                    when (s) {
                        EnterExitState.PreEnter -> -90f
                        EnterExitState.Visible -> 0f
                        EnterExitState.PostExit -> 90f
                    }
                }
                val flip = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        rotationY = rot
                        cameraDistance = 14f * density
                    }
                if (lyricsOn) {
                    LyricsCoverPanel(lyrics, lyricsPos, onToggle = toggleView, onOpen = onOpenLyrics, modifier = flip)
                } else {
                    Cover(
                        t,
                        flip.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { toggleView() }
                    )
                }
            }
            // при смене трека название и исполнитель «уезжают» влево, новые приезжают справа
            AnimatedContent(
                targetState = Pair(
                    t?.title ?: "ничего не играет",
                    if (t != null) "${t.artist} · ${t.album}" else "выберите музыку в коллекции"
                ),
                transitionSpec = {
                    (slideInHorizontally(tween(320)) { it / 4 } + fadeIn(tween(320))) togetherWith
                        (slideOutHorizontally(tween(200)) { -it / 4 } + fadeOut(tween(200)))
                },
                label = "nowTitle"
            ) { (title, sub) ->
                Column(Modifier.padding(top = 16.dp)) {
                    MText(title, 30, maxLines = 1)
                    MText(sub, 16, Modifier.padding(top = 2.dp), color = c.accent, maxLines = 1)
                }
            }
            SeekBar(pos, st.durationMs, onSeek, Modifier.padding(top = 8.dp), onScrub = { scrub = it })
            Row(Modifier.fillMaxWidth().offset(y = (-8).dp), horizontalArrangement = Arrangement.SpaceBetween) {
                val shown = scrub ?: pos
                MText(fmt(shown), 12, color = if (scrub != null) c.accent else c.subtle)
                MText("-" + fmt((st.durationMs - shown).coerceAtLeast(0L)), 12, color = c.subtle)
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(28.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MetroCircleButton(56.dp, Ic.PREV, 24.dp, c.fg, "предыдущий") { app.player.previous() }
                MetroCircleButton(
                    72.dp, if (st.isPlaying) Ic.PAUSE else Ic.PLAY, 30.dp, c.fg,
                    if (st.isPlaying) "пауза" else "играть"
                ) { app.player.togglePlay() }
                MetroCircleButton(56.dp, Ic.NEXT, 24.dp, c.fg, "следующий") { app.player.next() }
            }
            // быстрые режимы: перемешать · повтор · избранное · меню
            Row(
                Modifier.fillMaxWidth().padding(top = 6.dp),
                horizontalArrangement = if (app.store.quickBar) Arrangement.SpaceEvenly else Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (app.store.quickBar) {
                    RoundIconButton(Ic.SHUFFLE, if (st.shuffle) c.accent else c.subtle, "перемешать") { app.player.toggleShuffle() }
                    RoundIconButton(
                        if (st.repeatMode == Player.REPEAT_MODE_ONE) Ic.REPEAT_ONE else Ic.REPEAT,
                        if (st.repeatMode != Player.REPEAT_MODE_OFF) c.accent else c.subtle,
                        "повтор"
                    ) { app.player.cycleRepeat() }
                    RoundIconButton(Ic.HEART, if (fav) c.accent else c.subtle, "избранное", filled = fav) {
                        if (t != null) app.store.toggleFavorite(t.id)
                    }
                }
                Box(Modifier.size(52.dp), contentAlignment = Alignment.Center) { Ellipsis(c.subtle, onMore) }
            }
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                if (!showLyrics) LyricsStrip(lyrics, lyricsPos, onOpenLyrics)
            }
        }
    }
}

@Composable
private fun QueuePage(app: MetroApp, st: PlayerState, onMore: (Int) -> Unit) {
    // индексы треков в том порядке, в котором они реально заиграют (при shuffle он не совпадает со списком)
    val order = st.order.filter { it in st.queue.indices }
    if (order.isEmpty()) {
        EmptyHint("очередь пуста")
        return
    }
    LazyColumn(Modifier.fillMaxSize()) {
        item {
            MText(
                "в очереди: ${plural(order.size, "трек", "трека", "треков")} · удерживайте трек для меню", 13,
                Modifier.padding(start = 24.dp, end = 24.dp, bottom = 6.dp), color = Metro.colors.subtle
            )
        }
        itemsIndexed(order) { i, idx ->
            val t = st.queue[idx]
            TrackRow(
                title = t.title,
                subtitle = "${t.artist} · ${fmt(t.durationMs)}",
                highlighted = i == 0 && st.currentIndex >= 0,
                onClick = { app.player.seekToIndex(idx) },
                onLongClick = { onMore(idx) },
                onMore = { onMore(idx) },
                enterIndex = i
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
                onClick = { app.player.playTracks(tracks, i) },
                enterIndex = i
            )
        }
    }
}
