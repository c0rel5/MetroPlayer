package com.metroplayer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import android.os.Build
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import com.metroplayer.R
import com.metroplayer.MetroApp
import com.metroplayer.data.Lyrics
import com.metroplayer.data.LrcParser

/** Текст текущего трека (null — нет). Перечитывается при смене трека и при сохранении текста. */
@Composable
fun rememberLyrics(app: MetroApp, trackId: Long?): Lyrics? {
    val v = app.lyrics.version
    return produceState<Lyrics?>(null, trackId, v) {
        value = null
        value = if (trackId == null) null else app.lyrics.load(trackId)?.let { LrcParser.parse(it) }
    }.value
}

/** Гифка вместо «♪», когда в текущей строке нет слов (проигрыш/вступление). */
@Composable
fun NoteGif(size: Dp, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val loader = remember {
        ImageLoader.Builder(ctx)
            .components {
                if (Build.VERSION.SDK_INT >= 28) add(ImageDecoderDecoder.Factory()) else add(GifDecoder.Factory())
            }
            .build()
    }
    AsyncImage(
        model = R.raw.note_gif,
        imageLoader = loader,
        contentDescription = null,
        modifier = modifier.size(size)
    )
}

private fun activeIndex(l: Lyrics, posMs: Long): Int =
    if (l.synced) l.lines.indexOfLast { it.timeMs <= posMs } else -1

/** Вариант 1: две строки под кнопками — текущая и следующая. Тап открывает вкладку «текст». */
@Composable
fun LyricsStrip(lyrics: Lyrics?, pos: Long, onOpen: () -> Unit) {
    val c = Metro.colors
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onOpen)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        when {
            lyrics == null -> MText("текст: найти…", 16, color = c.inactive)
            !lyrics.synced -> MText("текст песни · открыть", 16, color = c.subtle)
            else -> {
                val i = activeIndex(lyrics, pos)
                val cur = lyrics.lines.getOrNull(i)?.text.orEmpty()
                val next = lyrics.lines.getOrNull(i + 1)?.text.orEmpty()
                if (cur.isEmpty()) NoteGif(48.dp)
                else MText(cur, 22, Modifier.fillMaxWidth(), color = c.accent, maxLines = 2, align = TextAlign.Center)
                MText(next, 16, Modifier.fillMaxWidth().padding(top = 4.dp), color = c.subtle, maxLines = 1, align = TextAlign.Center)
            }
        }
    }
}

/**
 * Вариант 2: текст вместо обложки. Синхронный — несколько строк вокруг текущей,
 * обычный — прокручиваемый. Тап возвращает обложку.
 */
@Composable
fun LyricsCoverPanel(lyrics: Lyrics?, pos: Long, onToggle: () -> Unit, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val c = Metro.colors
    Box(
        modifier
            .skinBg(c.fg.copy(alpha = 0.07f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onToggle)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        when {
            lyrics == null -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                MText("текста пока нет", 20, color = c.subtle)
                MText(
                    "найти текст →", 18,
                    Modifier
                        .padding(top = 12.dp)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onOpen),
                    color = c.accent
                )
            }
            !lyrics.synced -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                lyrics.lines.forEach { MText(it.text.ifEmpty { " " }, 18, Modifier.fillMaxWidth(), color = c.fg) }
            }
            else -> {
                val i = activeIndex(lyrics, pos)
                val from = (i - 2).coerceAtLeast(0)
                val to = (from + 5).coerceAtMost(lyrics.lines.size)
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    for (k in from until to) {
                        val on = k == i
                        if (on && lyrics.lines[k].text.isEmpty()) {
                            NoteGif(72.dp, Modifier.padding(vertical = 5.dp))
                            continue
                        }
                        MText(
                            lyrics.lines[k].text.ifEmpty { "♪" }, if (on) 24 else 18,
                            Modifier.fillMaxWidth().padding(vertical = 5.dp),
                            weight = if (on) FontWeight.SemiBold else FontWeight.Light,
                            color = if (on) c.accent else c.inactive,
                            align = TextAlign.Center, maxLines = 3
                        )
                    }
                }
            }
        }
    }
}
