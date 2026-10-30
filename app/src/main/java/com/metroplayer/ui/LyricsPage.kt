package com.metroplayer.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
            .border(2.dp, if (enabled) c.fg else c.inactive)
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

    LaunchedEffect(track?.id) {
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
    val active = if (lyrics.synced) lines.indexOfLast { it.timeMs <= pos } else -1
    val listState = rememberLazyListState()

    LaunchedEffect(active) {
        if (active >= 0) listState.animateScrollToItem((active - 2).coerceAtLeast(0))
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        state = listState,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 24.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        item {
            MText(
                if (lyrics.synced) "синхронизировано" else "обычный текст",
                12, color = c.subtle, weight = FontWeight.SemiBold, spacing = 2f
            )
        }
        itemsIndexed(lines) { i, l ->
            val on = i == active
            MText(
                l.text.ifEmpty { "♪" }, if (lyrics.synced) 26 else 20,
                Modifier
                    .fillMaxWidth()
                    .then(
                        if (lyrics.synced) {
                            Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                app.player.seekTo(l.timeMs)
                            }
                        } else {
                            Modifier
                        }
                    ),
                color = when {
                    !lyrics.synced -> c.fg
                    on -> c.accent
                    else -> c.inactive
                },
                weight = if (on) FontWeight.SemiBold else FontWeight.Light
            )
        }
        item {
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
    }
}
