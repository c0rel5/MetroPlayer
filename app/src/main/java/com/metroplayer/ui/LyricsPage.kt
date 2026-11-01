package com.metroplayer.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.Alignment
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.metroplayer.MetroApp
import com.metroplayer.data.LrcParser
import com.metroplayer.player.PlayerState
import kotlinx.coroutines.launch

@Composable
private fun TextButton(label: String, enabled: Boolean = true, onClick: () -> Unit) {
    val c = Metro.colors
    MText(
        label, 18,
        Modifier
            .padding(top = 14.dp)
            .metroPress(onClick = { if (enabled) onClick() })
            .skinBorder(2.dp, if (enabled) c.fg else c.inactive)
            .padding(horizontal = 22.dp, vertical = 10.dp),
        color = if (enabled) c.fg else c.inactive
    )
}

@Composable
private fun LinkText(label: String, onClick: () -> Unit) {
    MText(
        label, 18,
        Modifier
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(vertical = 8.dp),
        color = Metro.colors.subtle
    )
}

/** Вкладка «текст»: обычный или синхронный (LRC) текст песни. */
@Composable
fun LyricsPage(app: MetroApp, st: PlayerState, pos: Long) {
    val c = Metro.colors
    val track = st.current
    val scope = rememberCoroutineScope()

    var raw by remember(track?.id) { mutableStateOf<String?>(null) }
    var loaded by remember(track?.id) { mutableStateOf(false) }
    var busy by remember(track?.id) { mutableStateOf(false) }
    var msg by remember(track?.id) { mutableStateOf<String?>(null) }

    LaunchedEffect(track?.id, app.lyrics.version) {
        raw = track?.let { app.lyrics.load(it.id) }
        loaded = true
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val tr = track
        if (uri != null && tr != null) {
            scope.launch {
                val txt = app.lyrics.readUri(uri)
                if (txt.isNullOrBlank()) {
                    msg = "не удалось прочитать файл"
                } else {
                    app.lyrics.save(tr.id, txt)
                    raw = txt
                    msg = null
                }
            }
        }
    }

    val search: () -> Unit = {
        val tr = track
        if (tr != null && !busy) {
            busy = true
            msg = null
            scope.launch {
                val res = app.lyrics.fetchOnline(tr)
                busy = false
                if (res == null) {
                    msg = "не найдено (или нет сети)"
                } else {
                    app.lyrics.save(tr.id, res)
                    raw = res
                }
            }
        }
    }

    if (track == null) {
        EmptyHint("ничего не играет")
        return
    }
    if (!loaded) return

    val text = raw
    if (text == null) {
        Column(Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 8.dp)) {
            MText("для этого трека текста пока нет", 20, color = c.subtle)
            TextButton(if (busy) "поиск…" else "найти в интернете", enabled = !busy, onClick = search)
            TextButton("выбрать файл .lrc / .txt") { picker.launch(arrayOf("*/*")) }
            msg?.let { MText(it, 16, Modifier.padding(top = 16.dp), color = c.accent) }
            MText(
                "Синхронный текст подсвечивает строки по ходу песни. Нажмите на строку, чтобы перемотать к ней.",
                14, Modifier.padding(top = 24.dp), color = c.subtle
            )
        }
        return
    }

    val lyrics = remember(text) { LrcParser.parse(text) }
    val lines = lyrics.lines
    val selectMode = app.store.lyricsMode == 1 || !lyrics.synced

    val actions: @Composable () -> Unit = {
        Column(Modifier.padding(top = 20.dp)) {
            LinkText(if (busy) "поиск…" else "найти заново") { search() }
            LinkText("выбрать файл .lrc / .txt") { picker.launch(arrayOf("*/*")) }
            LinkText("удалить текст") {
                scope.launch {
                    app.lyrics.delete(track.id)
                    raw = null
                }
            }
            msg?.let { MText(it, 16, Modifier.padding(top = 8.dp), color = c.accent) }
        }
    }

    Column(Modifier.fillMaxSize()) {
        // переключатель режима
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            listOf("синхронно" to 0, "выделение" to 1).forEach { (label, mode) ->
                MText(
                    label, 18,
                    Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                        app.store.chooseLyricsMode(mode)
                    },
                    color = if (app.store.lyricsMode == mode) c.accent else c.inactive
                )
            }
        }

        if (!selectMode) {
            val shift = app.lyrics.offset(track.id)
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MText("сдвиг: " + (if (shift > 0) "+" else "") + (shift / 1000.0) + " с", 14, color = c.subtle)
                MText(
                    "−0.5", 18,
                    Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                        app.lyrics.nudge(track.id, -500)
                    },
                    color = c.fg
                )
                MText(
                    "+0.5", 18,
                    Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                        app.lyrics.nudge(track.id, 500)
                    },
                    color = c.fg
                )
            }
        }

        if (selectMode) {
            // Статичный текст: долгое нажатие выделяет слова, можно копировать.
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 8.dp)
            ) {
                SelectionContainer {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        lines.forEach { l ->
                            MText(l.text.ifEmpty { " " }, 20, Modifier.fillMaxWidth(), color = c.fg)
                        }
                    }
                }
                actions()
            }
        } else {
            val shift = app.lyrics.offset(track.id)
            val active = lines.indexOfLast { it.timeMs <= pos + shift }
            val listState = rememberLazyListState()
            // Плавная прокрутка: активная строка мягко «плывёт» примерно на треть высоты экрана
            LaunchedEffect(active) {
                if (active < 0) return@LaunchedEffect
                val info = listState.layoutInfo
                val viewport = (info.viewportEndOffset - info.viewportStartOffset).toFloat()
                val item = info.visibleItemsInfo.firstOrNull { it.index == active }
                if (item != null) {
                    val delta = item.offset + item.size / 2f - viewport * 0.35f
                    listState.animateScrollBy(delta, tween(650, easing = FastOutSlowInEasing))
                } else {
                    listState.animateScrollToItem(active, -(viewport * 0.3f).toInt())
                }
            }
            LazyColumn(
                Modifier.weight(1f).fillMaxWidth(),
                state = listState,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 240.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                itemsIndexed(lines) { i, l ->
                    val on = i == active
                    if (on && l.text.isEmpty()) {
                        NoteGif(96.dp, Modifier.padding(vertical = 4.dp))
                    } else {
                        // цвет и размер строки меняются плавно
                        val lineColor by animateColorAsState(if (on) c.accent else c.inactive, tween(400), label = "lyricColor")
                        val lineScale by animateFloatAsState(if (on) 1f else 0.8f, tween(400), label = "lyricScale")
                        MText(
                            l.text.ifEmpty { "♪" }, 26,
                            Modifier
                                .fillMaxWidth()
                                .graphicsLayer {
                                    scaleX = lineScale
                                    scaleY = lineScale
                                    transformOrigin = TransformOrigin(0f, 0.5f)
                                }
                                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                    app.player.seekTo(l.timeMs)
                                },
                            color = lineColor
                        )
                    }
                }
                item { actions() }
            }
        }
    }
}
