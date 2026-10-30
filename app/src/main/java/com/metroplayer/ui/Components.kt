package com.metroplayer.ui

import android.content.Context
import android.graphics.BitmapFactory
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.metroplayer.MetroApp
import com.metroplayer.data.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.roundToInt

/** SVG-пути иконок из концепта (viewBox 24×24, обводка 2). */
object Ic {
    const val PREV = "M7 5v14M19 5L9 12l10 7z"
    const val NEXT = "M17 5v14M5 5l10 7L5 19z"
    const val PAUSE = "M8 5v14M16 5v14"
    const val PLAY = "M8 5l11 7-11 7z"
    const val PLUS = "M12 4v16M4 12h16"
    const val PLAY_CIRCLE = "M3 12a9 9 0 1 0 18 0a9 9 0 1 0-18 0M10 8.5l5 3.5-5 3.5z"
    const val PAUSE_CIRCLE = "M3 12a9 9 0 1 0 18 0a9 9 0 1 0-18 0M10 9v6M14 9v6"
    const val RADIO = "M3 9h18v12H3zM7 9l9-5M5.5 15a3.5 3.5 0 1 0 7 0a3.5 3.5 0 1 0-7 0M15 13h3M15 17h3"
    const val NOTE = "M9 18V5l11-2v13M9 18a3 3 0 1 1-6 0a3 3 0 1 1 6 0M20 16a3 3 0 1 1-6 0a3 3 0 1 1 6 0"
}

@Composable
fun MetroIcon(path: String, iconSize: Dp, color: Color, modifier: Modifier = Modifier) {
    val parsed = remember(path) { PathParser().parsePathString(path).toPath() }
    Canvas(modifier.size(iconSize)) {
        val s = this.size.minDimension / 24f
        withTransform({ scale(s, s, Offset.Zero) }) {
            drawPath(parsed, color, style = Stroke(width = 2f, cap = StrokeCap.Butt, join = StrokeJoin.Miter))
        }
    }
}

/**
 * Metro-нажатие: плитка слегка уменьшается, а при tilt=true ещё и наклоняется
 * в сторону точки касания.
 */
@Composable
fun Modifier.metroPress(
    tilt: Boolean = false,
    pressScale: Float = 0.94f,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit
): Modifier {
    var pressed by remember { mutableStateOf(false) }
    var pos by remember { mutableStateOf(Offset.Zero) }
    var sz by remember { mutableStateOf(IntSize.Zero) }
    val scale by animateFloatAsState(if (pressed) pressScale else 1f, tween(120), label = "press")
    val nx = if (sz.width > 0) (pos.x / sz.width - 0.5f) * 2f else 0f
    val ny = if (sz.height > 0) (pos.y / sz.height - 0.5f) * 2f else 0f
    val ry by animateFloatAsState(if (pressed && tilt) nx * 10f else 0f, tween(120), label = "ry")
    val rx by animateFloatAsState(if (pressed && tilt) -ny * 10f else 0f, tween(120), label = "rx")
    val click by rememberUpdatedState(onClick)
    val longClick by rememberUpdatedState(onLongClick)
    return this
        .onSizeChanged { sz = it }
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
            rotationX = rx
            rotationY = ry
            cameraDistance = 16f * density
        }
        .pointerInput(onLongClick != null) {
            detectTapGestures(
                onPress = { o ->
                    pos = o
                    pressed = true
                    tryAwaitRelease()
                    pressed = false
                },
                onLongPress = if (onLongClick != null) {
                    { _: Offset -> longClick?.invoke() }
                } else {
                    null
                },
                onTap = { click() }
            )
        }
}

@Composable
fun MetroCircleButton(
    size: Dp,
    path: String,
    iconSize: Dp,
    color: Color,
    onClick: () -> Unit
) {
    Box(
        Modifier
            .size(size)
            .metroPress(onClick = onClick)
            .border(2.dp, color, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        MetroIcon(path, iconSize, color)
    }
}

@Composable
fun Ellipsis(color: Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
    MText(
        "···", 28,
        modifier
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        weight = FontWeight.Normal, color = color, spacing = 4f, lh = 1f
    )
}

/* ───────────────────────── обложка ───────────────────────── */

private fun loadCover(context: Context, t: Track): ImageBitmap? = try {
    val bmp = if (Build.VERSION.SDK_INT >= 29) {
        context.contentResolver.loadThumbnail(t.uri, android.util.Size(640, 640), null)
    } else {
        context.contentResolver.openInputStream(t.artUri)?.use { BitmapFactory.decodeStream(it) }
    }
    bmp?.asImageBitmap()
} catch (e: Exception) {
    null
}

/** Квадрат цвета акцента с концентрическими кругами (как в концепте), либо настоящая обложка. */
@Composable
fun Cover(track: Track?, modifier: Modifier = Modifier, bg: Color = Metro.colors.accent) {
    val context = LocalContext.current
    val bmp by produceState<ImageBitmap?>(initialValue = null, track?.id) {
        value = if (track == null) null else withContext(Dispatchers.IO) { loadCover(context, track) }
    }
    Box(modifier.background(bg), contentAlignment = Alignment.Center) {
        val b = bmp
        if (b != null) {
            Image(bitmap = b, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            Canvas(Modifier.fillMaxSize(0.76f)) {
                val s = this.size.minDimension / 260f
                val c = Offset(this.size.width / 2f, this.size.height / 2f)
                drawCircle(Color.White, 120f * s, c, style = Stroke(2f * s))
                drawCircle(Color.White, 80f * s, c, alpha = 0.6f, style = Stroke(2f * s))
                drawCircle(Color.White, 40f * s, c, alpha = 0.4f, style = Stroke(2f * s))
                drawLine(Color.White, Offset(c.x - 120f * s, c.y), Offset(c.x + 120f * s, c.y), 2f * s, alpha = 0.3f)
            }
        }
    }
}

/* ───────────────────────── перемотка ───────────────────────── */

@Composable
fun SeekBar(positionMs: Long, durationMs: Long, onSeek: (Long) -> Unit, modifier: Modifier = Modifier) {
    val c = Metro.colors
    var dragFrac by remember { mutableStateOf<Float?>(null) }
    val seek by rememberUpdatedState(onSeek)
    val frac = dragFrac ?: if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    Box(
        modifier
            .fillMaxWidth()
            .height(32.dp)
            .pointerInput(durationMs) {
                detectTapGestures { o -> seek((o.x / size.width * durationMs).toLong()) }
            }
            .pointerInput(durationMs) {
                detectHorizontalDragGestures(
                    onDragStart = { o -> dragFrac = (o.x / size.width).coerceIn(0f, 1f) },
                    onDragEnd = {
                        dragFrac?.let { seek((it * durationMs).toLong()) }
                        dragFrac = null
                    },
                    onDragCancel = { dragFrac = null },
                    onHorizontalDrag = { change, _ -> dragFrac = (change.position.x / size.width).coerceIn(0f, 1f) }
                )
            }
    ) {
        Canvas(Modifier.fillMaxWidth().height(32.dp)) {
            val y = this.size.height / 2f
            val w = this.size.width
            drawLine(c.track, Offset(0f, y), Offset(w, y), 2.dp.toPx())
            drawLine(c.accent, Offset(0f, y), Offset(w * frac, y), 2.dp.toPx())
            if (dragFrac != null) {
                drawRect(c.accent, Offset(w * frac - 5.dp.toPx(), y - 10.dp.toPx()), Size(10.dp.toPx(), 20.dp.toPx()))
            }
        }
    }
}

/* ───────────────────────── pivot ───────────────────────── */

@Composable
fun MetroPivot(
    state: PagerState,
    caption: String,
    titles: List<String>,
    modifier: Modifier = Modifier,
    content: @Composable (Int) -> Unit
) {
    val c = Metro.colors
    val scope = rememberCoroutineScope()
    val widths = remember(titles.size) { mutableStateListOf<Int>().also { l -> repeat(titles.size) { l.add(0) } } }
    val gapPx = with(LocalDensity.current) { 20.dp.roundToPx() }

    Column(modifier) {
        MText(caption, 12, Modifier.padding(start = 24.dp, top = 24.dp), FontWeight.SemiBold, c.subtle, spacing = 2f)
        Box(Modifier.fillMaxWidth().clipToBounds().padding(top = 4.dp, bottom = 12.dp)) {
            Row(
                Modifier
                    .wrapContentWidth(Alignment.Start, unbounded = true)
                    .padding(start = 24.dp)
                    .offset {
                        val pos = (state.currentPage + state.currentPageOffsetFraction)
                            .coerceIn(0f, (titles.size - 1).toFloat())
                        val lo = floor(pos).toInt()
                        val hi = min(lo + 1, titles.size - 1)
                        val f = pos - lo
                        fun start(i: Int): Int {
                            var s = 0
                            for (k in 0 until i) s += widths[k] + gapPx
                            return s
                        }
                        IntOffset(-(start(lo) + (start(hi) - start(lo)) * f).roundToInt(), 0)
                    }
            ) {
                titles.forEachIndexed { i, t ->
                    val a = 1f - abs(state.currentPage + state.currentPageOffsetFraction - i).coerceIn(0f, 1f)
                    MText(
                        t, 46,
                        Modifier
                            .onSizeChanged { widths[i] = it.width }
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                scope.launch { state.animateScrollToPage(i) }
                            },
                        color = lerp(c.inactive, c.fg, a), maxLines = 1, softWrap = false, lh = 1.1f
                    )
                    Spacer(Modifier.width(20.dp))
                }
            }
        }
        HorizontalPager(state = state, modifier = Modifier.weight(1f).fillMaxWidth()) { page -> content(page) }
    }
}

/* ───────────────────────── списки ───────────────────────── */

@Composable
fun TrackRow(
    title: String,
    subtitle: String,
    highlighted: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    val c = Metro.colors
    Column(
        Modifier
            .fillMaxWidth()
            .height(64.dp)
            .metroPress(pressScale = 0.98f, onLongClick = onLongClick, onClick = onClick)
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        MText(title, 22, color = if (highlighted) c.accent else c.fg, maxLines = 1)
        MText(subtitle, 14, color = c.subtle, maxLines = 1)
    }
}

/* ───────────────────────── нижняя панель ───────────────────────── */

@Composable
fun PlayerButtons(app: MetroApp) {
    val st = app.player.state
    MetroCircleButton(44.dp, Ic.PREV, 24.dp, Color.White) { app.player.previous() }
    MetroCircleButton(44.dp, if (st.isPlaying) Ic.PAUSE else Ic.PLAY, 24.dp, Color.White) { app.player.togglePlay() }
    MetroCircleButton(44.dp, Ic.NEXT, 24.dp, Color.White) { app.player.next() }
}

@Composable
fun MetroBottomBar(
    onMore: () -> Unit,
    modifier: Modifier = Modifier,
    onMid: (() -> Unit)? = null,
    left: @Composable RowScope.() -> Unit
) {
    Column(modifier.fillMaxWidth().background(BarColor).navigationBarsPadding()) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(76.dp)
                .then(
                    if (onMid != null) {
                        Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onMid)
                    } else {
                        Modifier
                    }
                )
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                left()
            }
            Ellipsis(BarSubtle, onMore)
        }
    }
}

/* ───────────────────────── меню приложения ───────────────────────── */

class MenuEntry(val label: String, val onClick: () -> Unit)

@Composable
fun MetroMenu(visible: Boolean, items: List<MenuEntry>, onDismiss: () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        AnimatedVisibility(visible, enter = fadeIn(tween(150)), exit = fadeOut(tween(150))) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color(0x99000000))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss)
            )
        }
        AnimatedVisibility(
            visible,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(tween(200)) { it },
            exit = slideOutVertically(tween(150)) { it }
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(BarColor)
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                items.forEach { e ->
                    MText(
                        e.label, 20,
                        Modifier
                            .fillMaxWidth()
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                onDismiss()
                                e.onClick()
                            }
                            .padding(vertical = 12.dp),
                        color = Color.White, align = androidx.compose.ui.text.style.TextAlign.End
                    )
                }
            }
        }
    }
}
