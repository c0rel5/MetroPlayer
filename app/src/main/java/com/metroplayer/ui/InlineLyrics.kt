package com.metroplayer.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.metroplayer.MetroApp
import com.metroplayer.R
import com.metroplayer.data.LrcParser
import com.metroplayer.data.Lyrics

/** Текст текущего трека (null — нет). Перечитывается при смене трека и при сохранении текста. */
@Composable
fun rememberLyrics(app: MetroApp, trackId: Long?): Lyrics? {
    val v = app.lyrics.version
    return produceState<Lyrics?>(null, trackId, v) {
        value = null
        value = if (trackId == null) null else app.lyrics.load(trackId)?.let { LrcParser.parse(it) }
    }.value
}

/**
 * Гифка вместо «♪», когда в текущей строке нет слов (проигрыш/вступление).
 * Если пользователь выбрал свою гифку (настройки → «гифка вместо ноты»), показывается она.
 */
@Composable
fun NoteGif(size: Dp, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val store = (ctx.applicationContext as MetroApp).store
    val custom = remember(store.noteGif) { store.noteGifFile() }
    AsyncImage(
        model = custom ?: R.raw.note_gif,
        imageLoader = GifLoader.get(ctx),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = modifier.size(size)
    )
}

/**
 * Строка текста, у которой при [play] слова загораются по очереди (karaoke-стиль).
 * Когда [play] выключен, показывается целиком.
 */
@Composable
fun RevealMText(
    text: String,
    size: Int,
    modifier: Modifier = Modifier,
    weight: FontWeight = FontWeight.Light,
    color: androidx.compose.ui.graphics.Color = Metro.colors.fg,
    maxLines: Int = Int.MAX_VALUE,
    align: TextAlign = TextAlign.Start,
    play: Boolean = true
) {
    // ключ включает play: когда строка становится активной, анимация создаётся сразу с нуля — без мерцания
    val progress = remember(text, play) { Animatable(if (play) 0f else 1f) }
    LaunchedEffect(progress) {
        if (progress.value < 1f) {
            val words = text.trim().split(Regex("\\s+")).size
            progress.animateTo(1f, tween((380 + words * 70).coerceAtMost(1000), easing = LinearEasing))
        }
    }
    MText(text, size, modifier, weight, color, maxLines = maxLines, align = align, progress = progress.value)
}

private fun activeIndex(l: Lyrics, posMs: Long): Int =
    if (l.synced) l.lines.indexOfLast { it.timeMs <= posMs } else -1

/** Вариант 1: две строки под кнопками — текущая и следующая. Тап открывает вкладку «текст». */
@Composable
fun LyricsStrip(lyrics: Lyrics?, pos: Long, onOpen: () -> Unit, classic: Boolean = false) {
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
                // Анимация привязана к номеру строки, а не к её тексту; блок всегда одной высоты,
                // поэтому ничего не прыгает и не дёргает кнопки выше. Новая строка просто проявляется.
                AnimatedContent(
                    targetState = i,
                    modifier = Modifier.fillMaxWidth().height(88.dp),
                    contentAlignment = Alignment.Center,
                    transitionSpec = {
                        if (classic) {
                            (slideInVertically(tween(380)) { it / 2 } + fadeIn(tween(380))) togetherWith
                                (slideOutVertically(tween(380)) { -it / 2 } + fadeOut(tween(220)))
                        } else {
                            fadeIn(tween(300, delayMillis = 90)) togetherWith fadeOut(tween(160))
                        }
                    },
                    label = "lyricStrip"
                ) { idx ->
                    val curLine = lyrics.lines.getOrNull(idx)
                    val nextLine = lyrics.lines.getOrNull(idx + 1)
                    Column(
                        Modifier.fillMaxWidth().height(88.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        when {
                            curLine == null -> Spacer(Modifier.height(1.dp))
                            curLine.text.isEmpty() -> NoteGif(48.dp)
                            else -> MText(curLine.text, 22, Modifier.fillMaxWidth(), color = c.accent, maxLines = 2, align = TextAlign.Center)
                        }
                        if (nextLine != null) {
                            if (nextLine.text.isEmpty()) {
                                NoteGif(24.dp, Modifier.padding(top = 4.dp))
                            } else {
                                MText(nextLine.text, 16, Modifier.fillMaxWidth().padding(top = 4.dp), color = c.subtle, maxLines = 1, align = TextAlign.Center)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Вариант 2: текст вместо обложки. Синхронный — несколько строк вокруг текущей,
 * обычный — прокручиваемый. Тап возвращает обложку.
 */
@Composable
fun LyricsCoverPanel(
    lyrics: Lyrics?,
    pos: Long,
    onToggle: () -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
    classic: Boolean = false
) {
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
            else -> if (classic) ClassicLyrics(lyrics, pos) else GlideLyrics(lyrics, pos)
        }
    }
}

/** Новая анимация: строки плавно плывут к центру, слова активной строки загораются по очереди. */
@Composable
private fun GlideLyrics(lyrics: Lyrics, pos: Long) {
    val c = Metro.colors
    val iNow = activeIndex(lyrics, pos)
    val listState = rememberLazyListState()
    // Строки не «прыгают» окном, а плавно плывут: активная строка мягко уезжает в центр панели,
    // соседние уменьшаются и бледнеют по мере удаления от неё.
    LaunchedEffect(iNow, lyrics) {
        if (iNow < 0) {
            listState.scrollToItem(0)
            return@LaunchedEffect
        }
        val li = iNow + 1   // нулевой элемент списка — отступ сверху
        if (listState.layoutInfo.visibleItemsInfo.none { it.index == li }) listState.scrollToItem(li)
        val info = listState.layoutInfo
        val item = info.visibleItemsInfo.firstOrNull { it.index == li } ?: return@LaunchedEffect
        val center = (info.viewportStartOffset + info.viewportEndOffset) / 2f
        listState.animateScrollBy(item.offset + item.size / 2f - center, tween(600, easing = FastOutSlowInEasing))
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val pad = maxHeight / 2
        LazyColumn(
            Modifier.fillMaxSize(),
            state = listState,
            userScrollEnabled = false,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item { Spacer(Modifier.height(pad)) }
            itemsIndexed(lyrics.lines) { k, line ->
                val dist = if (iNow < 0) 3 else kotlin.math.abs(k - iNow)
                val on = dist == 0
                val targetScale = when (dist) {
                    0 -> 1f
                    1 -> 0.84f
                    else -> 0.72f
                }
                val targetAlpha = when (dist) {
                    0 -> 1f
                    1 -> 0.6f
                    2 -> 0.4f
                    else -> 0.25f
                }
                val lineScale by animateFloatAsState(targetScale, spring(dampingRatio = 0.7f, stiffness = 200f), label = "lineScale")
                val lineAlpha by animateFloatAsState(targetAlpha, tween(500), label = "lineAlpha")
                if (line.text.isEmpty()) {
                    Box(Modifier.fillMaxWidth().padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
                        NoteGif(
                            if (on) 72.dp else 40.dp,
                            Modifier.graphicsLayer {
                                scaleX = lineScale
                                scaleY = lineScale
                                alpha = lineAlpha
                            }
                        )
                    }
                } else {
                    RevealMText(
                        line.text, 22,
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .graphicsLayer {
                                scaleX = lineScale
                                scaleY = lineScale
                                alpha = lineAlpha
                            },
                        weight = if (on) FontWeight.SemiBold else FontWeight.Light,
                        color = if (on) c.accent else c.fg,
                        align = TextAlign.Center, maxLines = 3, play = on
                    )
                }
            }
            item { Spacer(Modifier.height(pad)) }
        }
    }
            }

/** Старая анимация: окно из пяти строк целиком выезжает снизу при смене строки. */
@Composable
private fun ClassicLyrics(lyrics: Lyrics, pos: Long) {
    val c = Metro.colors
    val iNow = activeIndex(lyrics, pos)
    AnimatedContent(
        targetState = iNow,
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
        transitionSpec = {
            (slideInVertically(tween(420)) { it / 5 } + fadeIn(tween(420))) togetherWith
                (slideOutVertically(tween(420)) { -it / 5 } + fadeOut(tween(260)))
        },
        label = "lyricCover"
    ) { i ->
        val from = (i - 2).coerceAtLeast(0)
        val to = (from + 5).coerceAtMost(lyrics.lines.size)
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            for (k in from until to) {
                val on = k == i
                if (lyrics.lines[k].text.isEmpty()) {
                    NoteGif(if (on) 72.dp else 36.dp, Modifier.padding(vertical = 5.dp))
                    continue
                }
                MText(
                    lyrics.lines[k].text, if (on) 24 else 18,
                    Modifier.fillMaxWidth().padding(vertical = 5.dp),
                    weight = if (on) FontWeight.SemiBold else FontWeight.Light,
                    color = if (on) c.accent else c.inactive,
                    align = TextAlign.Center, maxLines = 3
                )
            }
        }
    }
}
